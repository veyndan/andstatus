/*
 * Copyright (C) 2017 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.net.social;

import android.support.annotation.NonNull;

/** Activity in a sense of Activity Streams https://www.w3.org/TR/activitystreams-core/ */
public class MbActivity {
    public static final MbActivity EMPTY = new MbActivity();
    private TimelinePosition timelinePosition = TimelinePosition.EMPTY;
    private long timelineDate = 0;

    private MbUser actor = MbUser.EMPTY;
    private MbActivityType activityType = MbActivityType.EMPTY;

    // Objects of the Activity may be of several types...
    private MbMessage mbMessage = MbMessage.EMPTY;
    private MbUser mbUser = MbUser.EMPTY;
    private MbActivity mbActivity = MbActivity.EMPTY;

    @NonNull
    public static MbActivity fromMessage(@NonNull MbUser actor, @NonNull MbActivityType activityType,
                                  @NonNull MbMessage message) {
        MbActivity mbActivity = new MbActivity();
        mbActivity.actor = actor;
        mbActivity.setActivityType(activityType);
        mbActivity.mbMessage = message;
        return mbActivity;
    }

    @NonNull
    public static MbActivity fromUser(@NonNull MbUser actor, @NonNull MbActivityType activityType,
                                  @NonNull MbUser user) {
        MbActivity mbActivity = new MbActivity();
        mbActivity.actor = actor;
        mbActivity.setActivityType(activityType);
        mbActivity.mbUser = user;
        return mbActivity;
    }

    @NonNull
    public static MbActivity undo(@NonNull MbActivity activity) {
        MbActivity mbActivity = new MbActivity();
        mbActivity.actor = activity.getActor();
        mbActivity.mbActivity = activity;
        mbActivity.setActivityType(MbActivityType.UNDO);
        return mbActivity;
    }

    @NonNull
    public MbUser getActor() {
        if (!actor.isEmpty()) {
            return actor;
        }
        switch (getObjectType()) {
            case MESSAGE:
                return mbMessage.getActor();
            case USER:
                return mbUser.actor;
            default:
                return MbUser.EMPTY;
        }
    }

    @NonNull
    public MbActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(@NonNull MbActivityType activityType) {
        this.activityType = activityType;
    }

    public MbObjectType getObjectType() {
        if (!mbMessage.isEmpty()) {
            return MbObjectType.MESSAGE;
        } else if (!mbUser.isEmpty()) {
            return MbObjectType.USER;
        } else if (!mbActivity.isEmpty()) {
            return MbObjectType.ACTIVITY;
        } else {
            return MbObjectType.EMPTY;
        }
    }

    public boolean isEmpty() {
        return activityType == MbActivityType.EMPTY || getObjectType() == MbObjectType.EMPTY;
    }

    public TimelinePosition getTimelinePosition() {
        return timelinePosition;
    }

    public void setTimelinePosition(String strPosition) {
        this.timelinePosition = new TimelinePosition(strPosition);
    }

    public long getTimelineDate() {
        return timelineDate;
    }

    public void setTimelineDate(long timelineDate) {
        this.timelineDate = timelineDate;
    }

    @NonNull
    public MbMessage getMessage() {
        return mbMessage;
    }

    public void setMessage(MbMessage mbMessage) {
        this.mbMessage = mbMessage == null ? MbMessage.EMPTY : mbMessage;
    }

    @NonNull
    public MbUser getUser() {
        return mbUser;
    }

    public void setUser(MbUser mbUser) {
        this.mbUser = mbUser == null ? MbUser.EMPTY : mbUser;
    }

    @NonNull
    public MbActivity getActivity() {
        return mbActivity;
    }
}
