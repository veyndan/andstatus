/* 
 * Copyright (c) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under MyDatabaseConverterExecutor License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andstatus.app.data.converter

import android.app.Activity
import android.database.sqlite.SQLiteDatabase
import net.jcip.annotations.GuardedBy
import org.andstatus.app.MyActivity
import org.andstatus.app.R
import org.andstatus.app.backup.DefaultProgressListener
import org.andstatus.app.backup.ProgressLogger
import org.andstatus.app.context.MyContextHolder
import org.andstatus.app.data.checker.DataChecker
import org.andstatus.app.os.AsyncTaskLauncher
import org.andstatus.app.os.MyAsyncTask
import org.andstatus.app.service.MyServiceManager
import org.andstatus.app.util.MyLog
import org.andstatus.app.util.MyStringBuilder
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class DatabaseConverterController {
    private class AsyncUpgrade internal constructor(val upgradeRequestor: Activity, val isRestoring: Boolean) : MyAsyncTask<Void?, Void?, Void?>(PoolEnum.LONG_UI) {
        val progressLogger: ProgressLogger? = null
        override fun doInBackground2(aVoid: Void?): Void? {
            syncUpgrade()
            return null
        }

        private fun syncUpgrade() {
            var success = false
            success = try {
                progressLogger.logProgress(upgradeRequestor.getText(R.string.label_upgrading))
                doUpgrade()
            } finally {
                progressLogger.onComplete(success)
            }
        }

        private fun doUpgrade(): Boolean {
            var success = false
            var locUpgradeStarted = false
            try {
                synchronized(UPGRADE_LOCK) { mProgressLogger = progressLogger }
                MyLog.i(TAG, "Upgrade triggered by " + MyStringBuilder.Companion.objToTag(upgradeRequestor))
                MyServiceManager.Companion.setServiceUnavailable()
                 MyContextHolder.myContextHolder.release(Supplier { "doUpgrade" })
                // Upgrade will occur inside this call synchronously
                // TODO: Add completion stage instead of blocking...
                 MyContextHolder.myContextHolder.initializeDuringUpgrade(upgradeRequestor).getBlocking()
                synchronized(UPGRADE_LOCK) { shouldTriggerDatabaseUpgrade = false }
            } catch (e: Exception) {
                MyLog.i(TAG, "Failed to trigger database upgrade, will try later", e)
            } finally {
                synchronized(UPGRADE_LOCK) {
                    success = upgradeEndedSuccessfully
                    mProgressLogger = null
                    locUpgradeStarted = upgradeStarted
                    upgradeStarted = false
                    upgradeEndTime = 0L
                }
            }
            if (!locUpgradeStarted) {
                MyLog.v(TAG, "Upgrade didn't start")
            }
            if (success) {
                MyLog.i(TAG, "success " +  MyContextHolder.myContextHolder.getNow().state())
                onUpgradeSucceeded()
            }
            return success
        }

        private fun onUpgradeSucceeded() {
            MyServiceManager.Companion.setServiceUnavailable()
            if (! MyContextHolder.myContextHolder.getNow().isReady()) {
                 MyContextHolder.myContextHolder.release(Supplier { "onUpgradeSucceeded1" })
                 MyContextHolder.myContextHolder.initialize(upgradeRequestor).getBlocking()
            }
            MyServiceManager.Companion.setServiceUnavailable()
            MyServiceManager.Companion.stopService()
            if (isRestoring) return
            DataChecker.Companion.fixData(progressLogger, false, false)
             MyContextHolder.myContextHolder.release(Supplier { "onUpgradeSucceeded2" })
             MyContextHolder.myContextHolder.initialize(upgradeRequestor).getBlocking()
            MyServiceManager.Companion.setServiceAvailable()
        }

        init {
            progressLogger = if (upgradeRequestor is MyActivity) {
                val progressListener: ProgressLogger.ProgressListener = DefaultProgressListener(upgradeRequestor as MyActivity, R.string.label_upgrading, "ConvertDatabase")
                ProgressLogger(progressListener)
            } else {
                ProgressLogger.Companion.getEmpty("ConvertDatabase")
            }
        }
    }

    fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (!shouldTriggerDatabaseUpgrade) {
            MyLog.v(this, "onUpgrade - Trigger not set yet")
            throw IllegalStateException("onUpgrade - Trigger not set yet")
        }
        synchronized(UPGRADE_LOCK) {
            shouldTriggerDatabaseUpgrade = false
            stillUpgrading()
        }
         MyContextHolder.myContextHolder.getNow().setInForeground(true)
        val databaseConverter = DatabaseConverter()
        val success = databaseConverter.execute(UpgradeParams(mProgressLogger, db, oldVersion, newVersion))
        synchronized(UPGRADE_LOCK) {
            upgradeEnded = true
            upgradeEndedSuccessfully = success
        }
        if (!success) {
            throw ApplicationUpgradeException(databaseConverter.converterError)
        }
    }

    internal class UpgradeParams(var progressLogger: ProgressLogger?, var db: SQLiteDatabase?, var oldVersion: Int, var newVersion: Int)
    companion object {
        private val TAG: String? = DatabaseConverterController::class.java.simpleName
        private val UPGRADE_LOCK: Any? = Any()

        @GuardedBy("upgradeLock")
        @Volatile
        private var shouldTriggerDatabaseUpgrade = false

        /**
         * Semaphore enabling uninterrupted system upgrade
         */
        @GuardedBy("upgradeLock")
        private var upgradeEndTime = 0L

        @GuardedBy("upgradeLock")
        private var upgradeStarted = false

        @GuardedBy("upgradeLock")
        private var upgradeEnded = false

        @GuardedBy("upgradeLock")
        private var upgradeEndedSuccessfully = false

        @GuardedBy("upgradeLock")
        private var mProgressLogger: ProgressLogger? = null
        const val SECONDS_BEFORE_UPGRADE_TRIGGERED = 5L
        const val UPGRADE_LENGTH_SECONDS_MAX = 90
        fun attemptToTriggerDatabaseUpgrade(upgradeRequestorIn: Activity) {
            val requestorName: String = MyStringBuilder.Companion.objToTag(upgradeRequestorIn)
            var skip = false
            if (isUpgrading()) {
                MyLog.v(TAG, "Attempt to trigger database upgrade by " + requestorName
                        + ": already upgrading")
                skip = true
            }
            if (!skip && ! MyContextHolder.myContextHolder.getNow().initialized()) {
                MyLog.v(TAG, "Attempt to trigger database upgrade by " + requestorName
                        + ": not initialized yet")
                skip = true
            }
            if (!skip && acquireUpgradeLock(requestorName)) {
                val asyncUpgrade = AsyncUpgrade(upgradeRequestorIn,  MyContextHolder.myContextHolder.isOnRestore())
                if ( MyContextHolder.myContextHolder.isOnRestore()) {
                    asyncUpgrade.syncUpgrade()
                } else {
                    AsyncTaskLauncher.Companion.execute(TAG, asyncUpgrade)
                }
            }
        }

        private fun acquireUpgradeLock(requestorName: String?): Boolean {
            var skip = false
            synchronized(UPGRADE_LOCK) {
                if (isUpgrading()) {
                    MyLog.v(TAG, "Attempt to trigger database upgrade by " + requestorName
                            + ": already upgrading")
                    skip = true
                }
                if (!skip && upgradeEnded) {
                    MyLog.v(TAG, "Attempt to trigger database upgrade by " + requestorName
                            + ": already completed " + if (upgradeEndedSuccessfully) " successfully" else "(failed)")
                    skip = true
                    if (!upgradeEndedSuccessfully) {
                        upgradeEnded = false
                    }
                }
                if (!skip) {
                    MyLog.v(TAG, "Upgrade lock acquired for $requestorName")
                    val startTime = System.currentTimeMillis()
                    upgradeEndTime = startTime + TimeUnit.SECONDS.toMillis(SECONDS_BEFORE_UPGRADE_TRIGGERED)
                    shouldTriggerDatabaseUpgrade = true
                }
            }
            return !skip
        }

        fun stillUpgrading() {
            var wasStarted: Boolean
            synchronized(UPGRADE_LOCK) {
                wasStarted = upgradeStarted
                upgradeStarted = true
                upgradeEndTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(UPGRADE_LENGTH_SECONDS_MAX.toLong())
            }
            MyLog.w(TAG, (if (wasStarted) "Still upgrading" else "Upgrade started") + ". Wait " + UPGRADE_LENGTH_SECONDS_MAX + " seconds")
        }

        fun isUpgradeError(): Boolean {
            synchronized(UPGRADE_LOCK) {
                if (upgradeEnded && !upgradeEndedSuccessfully) {
                    return true
                }
            }
            return false
        }

        fun isUpgrading(): Boolean {
            synchronized(UPGRADE_LOCK) {
                if (upgradeEndTime == 0L) {
                    return false
                }
                val currentTime = System.currentTimeMillis()
                if (currentTime > upgradeEndTime) {
                    MyLog.v(TAG, "Upgrade end time came")
                    upgradeEndTime = 0L
                    return false
                }
            }
            return true
        }
    }
}