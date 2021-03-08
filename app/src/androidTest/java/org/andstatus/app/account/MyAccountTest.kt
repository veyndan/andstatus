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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andstatus.app.account

import org.andstatus.app.context.DemoData
import org.andstatus.app.context.MyContext
import org.andstatus.app.context.MyContextHolder
import org.andstatus.app.context.TestSuite
import org.andstatus.app.origin.OriginType
import org.andstatus.app.util.MyLog
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import kotlin.Throws

class MyAccountTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        TestSuite.initializeWithAccounts(this)
    }

    @Test
    fun testNewAccountCreation() {
        createAccountOfOriginType("", "", OriginType.TWITTER)
        createAccountOfOriginType("testUser1", DemoData.demoData.twitterTestHostWithoutApiDot, OriginType.TWITTER)
        createAccountOfOriginType("", "", OriginType.PUMPIO)
        createAccountOfOriginType("test2User", "somepipe.example.com", OriginType.PUMPIO)
        createAccountOfOriginType("PeterPom", DemoData.demoData.gnusocialTestHost, OriginType.GNUSOCIAL)
        createAccountOfOriginType("", "", OriginType.ACTIVITYPUB)
        createAccountOfOriginType("AndStatus", "pleroma.site", OriginType.ACTIVITYPUB)
    }

    private fun createAccountOfOriginType(username: String?, host: String?, originType: OriginType?) {
        val uniqueName = if (username.isNullOrEmpty()) "" else "$username@$host"
        val myContext: MyContext =  MyContextHolder.myContextHolder.getNow()
        val logMsg = "Creating account '$uniqueName' for '$originType'"
        MyLog.v(this, logMsg)
        val origin = myContext.origins().fromOriginInAccountNameAndHost(originType.getTitle(), host)
        val accountNameString = uniqueName + AccountName.Companion.ORIGIN_SEPARATOR + origin.getOriginInAccountName(host)
        val accountName: AccountName = AccountName.Companion.fromAccountName(myContext, accountNameString)
        val builder: MyAccount.Builder = MyAccount.Builder.Companion.fromAccountName(accountName)
        Assert.assertEquals(logMsg, origin, builder.account.origin)
        Assert.assertEquals(logMsg, accountNameString, builder.account.accountName)
        Assert.assertEquals(logMsg, username, builder.account.username)
        if (uniqueName.isNullOrEmpty()) {
            Assert.assertEquals(logMsg, "", builder.account.webFingerId)
        } else {
            Assert.assertNotEquals(logMsg, uniqueName, builder.account.username)
            val indexOfAt = uniqueName.lastIndexOf("@")
            Assert.assertEquals(logMsg, uniqueName, builder.account.username +
                    "@" + uniqueName.substring(indexOfAt + 1))
            Assert.assertEquals(logMsg, uniqueName.toLowerCase(), builder.account.actor.webFingerId)
        }
    }

    @Test
    fun testUser() {
        val ma: MyAccount = DemoData.demoData.getMyAccount(DemoData.demoData.conversationAccountName)
        Assert.assertTrue(DemoData.demoData.conversationAccountName + " exists", ma.isValid)
        val accountActor = ma.actor
        Assert.assertTrue("Should be fully defined $accountActor", accountActor.isFullyDefined)
    }

    companion object {
        fun fixPersistentAccounts(myContext: MyContext?) {
            for (ma in myContext.accounts().get()) {
                MyAccountTest.Companion.fixAccountByName(myContext, ma.accountName)
            }
        }

        private fun fixAccountByName(myContext: MyContext?, accountName: String?) {
            val ma = myContext.accounts().fromAccountName(accountName)
            Assert.assertTrue("Account $accountName is valid", ma.isValid)
            if (ma.credentialsVerified == CredentialsVerificationStatus.SUCCEEDED) {
                return
            }
            val builder: MyAccount.Builder = MyAccount.Builder.Companion.fromAccountName(ma.oAccountName)
            builder.setCredentialsVerificationStatus(CredentialsVerificationStatus.SUCCEEDED)
            builder.saveSilently()
        }
    }
}