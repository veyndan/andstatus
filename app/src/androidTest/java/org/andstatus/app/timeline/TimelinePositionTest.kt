/*
 * Copyright (C) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andstatus.app.timeline

import android.content.Intent
import org.andstatus.app.activity.ActivityViewItem
import org.andstatus.app.context.MyContextHolder
import org.andstatus.app.context.TestSuite
import org.andstatus.app.data.DbUtils
import org.andstatus.app.net.social.Actor
import org.andstatus.app.origin.Origin
import org.andstatus.app.timeline.TimelinePositionTest
import org.andstatus.app.timeline.meta.TimelineType
import org.andstatus.app.util.MyLog
import org.junit.Assert
import org.junit.Test

class TimelinePositionTest : TimelineActivityTest<ActivityViewItem?>() {
    override fun getActivityIntent(): Intent? {
        MyLog.i(this, "setUp started")
        TestSuite.initializeWithData(this)
        MyLog.i(this, "setUp ended")
        return Intent(Intent.ACTION_VIEW,
                 MyContextHolder.myContextHolder.getNow().timelines().get(TimelineType.HOME, Actor.EMPTY,  Origin.EMPTY).getUri())
    }

    @Test
    @Throws(InterruptedException::class)
    fun shouldStoreTimelinePosition1() {
        oneTimelineOpening(1)
    }

    @Test
    @Throws(InterruptedException::class)
    fun shouldStoreTimelinePosition2() {
        oneTimelineOpening(2)
    }

    @Test
    @Throws(InterruptedException::class)
    fun shouldStoreTimelinePosition3() {
        oneTimelineOpening(3)
    }

    @Throws(InterruptedException::class)
    private fun oneTimelineOpening(iteration: Int) {
        val method = "oneTimelineOpening$iteration"
        TestSuite.waitForListLoaded(activity, 3)
        val testHelper = ListScreenTestHelper<TimelineActivity<*>?>(activity)
        val position1 = getFirstVisibleAdapterPosition()
        val listAdapter: BaseTimelineAdapter<*> = activity.listAdapter
        val item1 = listAdapter.getItem(position1)
        if (TimelinePositionTest.Companion.previousItem.nonEmpty) {
            val previousItemPosition = listAdapter.getPositionById(TimelinePositionTest.Companion.previousItem.getId())
            Assert.assertEquals("""; previous:${TimelinePositionTest.Companion.previousItem}
  ${if (previousItemPosition >= 0) "at position $previousItemPosition" else "not found now"}
current:$item1
  at position $position1""",
                    TimelinePositionTest.Companion.previousItem.getId(), item1.id)
        }
        val nextPosition = if (position1 + 5 >= listAdapter.count) 0 else position1 + 5
        testHelper.selectListPosition(method, nextPosition + activity.listView.headerViewsCount)
        DbUtils.waitMs(this, 2000)
        TimelinePositionTest.Companion.previousItem = listAdapter.getItem(getFirstVisibleAdapterPosition())
    }

    private fun getFirstVisibleAdapterPosition(): Int {
        val headers = activity.listView.headerViewsCount
        return Integer.max(activity.listView.firstVisiblePosition, headers) - headers
    }

    companion object {
        @Volatile
        private val previousItem: ViewItem<*>? = EmptyViewItem.EMPTY
    }
}