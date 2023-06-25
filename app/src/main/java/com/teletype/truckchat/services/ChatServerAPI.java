package com.teletype.truckchat.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;

import com.teletype.truckchat.db.ConversationsContract;
import com.teletype.truckchat.ui.news.NewsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatServerAPI {

    private static final String TAG = "TAG_" + ChatServerAPI.class.getSimpleName();

    private static final int TIMEOUT_CONNECT = 1000;
    private static final int TIMEOUT_READ = 30000;

    private static final String CONNECTION_ERROR = "Connection Error";

    private static final String DEVICE_TYPE_ANDROID = "Android";

    private static final String CHAT_SERVER_PRIMARY = "http://50.78.6.246";
    private static final String CHAT_SERVER_SECONDARY = "http://smarttruckroute.com";
    private static final String CHAT_SERVER_PATH = "/bb/v1";
    private static final String CHAT_SERVICE_DEVICE_REGISTER = "/device_register";
    private static final String CHAT_SERVICE_DEVICE_UPDATE = "/device_update";
    private static final String CHAT_SERVICE_DEVICE_MESSAGE = "/device_message";
    private static final String CHAT_SERVICE_DEVICE_POST_MESSAGE = "/device_post_message";
    private static final String CHAT_SERVICE_GET_ALL_REPLY_MESSAGE = "/get_all_reply_message";
    private static final String CHAT_SERVICE_GET_PREVIOUS_MESSAGES = "/get_previous_messages";
    private static final String CHAT_SERVICE_GET_NEWS = "/get_news";

    private static final String PROPERTY_CONTENT_TYPE = "Content-Type";
    private static final String PROPERTY_CONTENT_TYPE_APPLICATION_JSON = "application/json";

    private static final String PARAM_DEVICE_ID = "device_id" ;
    private static final String PARAM_DEVICE_GCM_ID = "device_gcm_id";
    private static final String PARAM_DEVICE_TYPE = "device_type" ;
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_MESSAGE = "message";
    private static final String PARAM_COUNTS = "counts";
    private static final String PARAM_MESSAGE_TOPIC = "original";
    private static final String PARAM_MESSAGE_REPLY_LIST = "messsage_reply_list";
    private static final String PARAM_SERVER_MSG_ID = "server_msg_id";
    private static final String PARAM_REPLY_MSG = "reply_msg";
    private static final String PARAM_SERVER_MSG_REPLY_ID = "server_msg_reply_id";
    private static final String PARAM_SERVER_MESSAGE_ID = "server_message_id";
    private static final String PARAM_USER_ID  = "user_id";
    private static final String PARAM_TIMESTAMP  = "timestamp";
    private static final String PARAM_LATITUDE  = "latitude";
    private static final String PARAM_LONGITUDE = "longitude";
    private static final String PARAM_MAX_POSTS = "max_posts";
    private static final String PARAM_MAX_HOURS = "max_hours";
    private static final String PARAM_MESSAGE_DEVICE_TYPE = "message_device_type";
    private static final String PARAM_MESSAGE_LATITUDE  = "message_latitude";
    private static final String PARAM_MESSAGE_LONGITUDE = "message_longitude";
    private static final String EMOJI_ID = "emoji_id";
    private static final String TITLE = "title";
    private static final String POSTED_DATE = "posted_date";
    private static final String LINK = "link";

    private abstract static class ParallelAsyncTask extends AsyncTask<Void, Void, Boolean> {

        public AsyncTask run() {
            return executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        public abstract boolean runNow();
    }

    public static class RegistrationAsyncTask extends ParallelAsyncTask {

        private final String serialNumber;
        private final String registrationId;

        public int status_code;
        public String status_message;
        public String user_id;

        public RegistrationAsyncTask(String serialNumber, String registrationId) {
            this.serialNumber = serialNumber;
            this.registrationId = registrationId;
        }

        public void onRegistrationResult(boolean success, int status_code, String status_message, String user_id) {
        }

        @Override
        public boolean runNow() {
            TTHttpConnection connection = getTTHttpConnection(CHAT_SERVICE_DEVICE_REGISTER);

            try {
                JSONObject entity = new JSONObject();
                entity.put(PARAM_DEVICE_ID, serialNumber);
                entity.put(PARAM_DEVICE_GCM_ID, registrationId);
                entity.put(PARAM_DEVICE_TYPE, DEVICE_TYPE_ANDROID);
                entity.put(PARAM_LATITUDE, 1.0); // this requirement should be removed for registration
                entity.put(PARAM_LONGITUDE, 1.0); // this requirement should be removed for registration
                //Log.d(TAG, "[SENT] " + entity.toString());
                connection.addStringEntity(entity.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                status_message = e.getMessage();
            }

            if (connection.execute()) {
                String result = connection.getResult();
                //Log.d(TAG, "[REPLY] " + result);

                try {
                    JSONObject jsonResult = new JSONObject(result);
                    status_code = jsonResult.getInt(PARAM_STATUS);

                    if (jsonResult.has(PARAM_MESSAGE)) {
                        status_message = jsonResult.getString(PARAM_MESSAGE);
                    } else {
                        status_message = "";
                    }

                    if (status_code == 200) {
                        if (jsonResult.has(PARAM_USER_ID)) {
                            user_id = jsonResult.getString(PARAM_USER_ID);
                        }
                    }

                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    status_message = e.getMessage();
                }
            } else {
                status_message = CONNECTION_ERROR;
            }

            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return runNow();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            onRegistrationResult(success, status_code, status_message, user_id);
        }
    }

    public static class DeviceUpdateAsyncTask extends ParallelAsyncTask {

        private final String serialNumber;
        private final double latitude;
        private final double longitude;

        public int status_code;
        public String status_message;

        public DeviceUpdateAsyncTask(String serialNumber, double latitude, double longitude) {
            this.serialNumber = serialNumber;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public void onDeviceUpdateResult(boolean success, int status_code, String status_message) {
        }

        @Override
        public boolean runNow() {
            TTHttpConnection connection = getTTHttpConnection(CHAT_SERVICE_DEVICE_UPDATE);

            try {
                JSONObject entity = new JSONObject();
                entity.put(PARAM_DEVICE_ID, serialNumber);
                entity.put(PARAM_DEVICE_TYPE, DEVICE_TYPE_ANDROID);
                entity.put(PARAM_LATITUDE, String.format(Locale.US, "%.6f", latitude));
                entity.put(PARAM_LONGITUDE, String.format(Locale.US, "%.6f", longitude));
                //Log.d(TAG, "[SENT] " + entity.toString());
                connection.addStringEntity(entity.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                status_message = e.getMessage();
            }

            if (connection.execute()) {
                String result = connection.getResult();
                //Log.d(TAG, "[REPLY] " + result);

                try {
                    JSONObject jsonResult = new JSONObject(result);
                    status_code = jsonResult.getInt(PARAM_STATUS);

                    if (jsonResult.has(PARAM_MESSAGE)) {
                        status_message = jsonResult.getString(PARAM_MESSAGE);
                    } else {
                        status_message = "";
                    }

                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    status_message = e.getMessage();
                }
            } else {
                status_message = CONNECTION_ERROR;
            }

            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return runNow();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            onDeviceUpdateResult(success, status_code, status_message);
        }
    }

    public static class DeviceMessageAsyncTask extends ParallelAsyncTask {

        private final String serialNumber;
        private final String messageBody;
        private final double latitude;
        private final double longitude;
        private final int emodji_id;

        public int status_code;
        public String status_message;
        public String conversation_id;

        public DeviceMessageAsyncTask(String serialNumber, String message,
                                      double latitude, double longitude, int emodji_id) {
            this.serialNumber = serialNumber;
            this.messageBody = message;
            this.latitude = latitude;
            this.longitude = longitude;
            this.emodji_id = emodji_id;
        }

        public void onDeviceMessageResult(boolean success, int status_code, String status_message, String conversation_id, int emodji_id) {
        }

        @Override
        public boolean runNow() {
            TTHttpConnection connection = getTTHttpConnection(CHAT_SERVICE_DEVICE_MESSAGE);

            try {
                JSONObject entity = new JSONObject();
                entity.put(PARAM_DEVICE_ID, serialNumber);
                entity.put(PARAM_MESSAGE_DEVICE_TYPE, DEVICE_TYPE_ANDROID);
                entity.put(PARAM_MESSAGE, messageBody);
                entity.put(PARAM_MESSAGE_LATITUDE, String.format(Locale.US, "%.6f", latitude));
                entity.put(PARAM_MESSAGE_LONGITUDE, String.format(Locale.US, "%.6f", longitude));
                entity.put(EMOJI_ID, emodji_id);
                //Log.d(TAG, "[SENT] " + entity.toString());
                connection.addStringEntity(entity.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                status_message = e.getMessage();
            }

            if (connection.execute()) {
                String result = connection.getResult();
                //Log.d(TAG, "[REPLY] " + result);

                try {
                    JSONObject jsonResult = new JSONObject(result);
                    status_code = jsonResult.getInt(PARAM_STATUS);

                    if (jsonResult.has(PARAM_MESSAGE)) {
                        status_message = jsonResult.getString(PARAM_MESSAGE);
                    } else {
                        status_message = "";
                    }

                    if (jsonResult.has(PARAM_SERVER_MSG_ID)) {
                        conversation_id = jsonResult.getString(PARAM_SERVER_MSG_ID);
                    }

                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    status_message = e.getMessage();
                }
            } else {
                status_message = CONNECTION_ERROR;
            }

            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return runNow();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            onDeviceMessageResult(success, status_code, status_message, conversation_id, emodji_id);
        }
    }

    public static class ReplyMessageAsyncTask extends ParallelAsyncTask {

        private final String messageBody;
        private final String userId;
        private final String conversationId;
        private final double latitude;
        private final double longitude;
        private final String emoji_id;

        public int status_code;
        public String status_message;

        public ReplyMessageAsyncTask(String message, String cid, String userId, double latitude, double longitude, String emoji_id) {
            this.messageBody = message;
            this.conversationId = cid;
            this.userId = userId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.emoji_id = emoji_id;
        }

        public void onReplyMessageResult(boolean success, int status_code, String status_message) {
        }

        @Override
        public boolean runNow() {
            TTHttpConnection connection = getTTHttpConnection(CHAT_SERVICE_DEVICE_POST_MESSAGE);

            try {
                JSONObject entity = new JSONObject();
                entity.put(PARAM_MESSAGE, messageBody);
                entity.put(PARAM_SERVER_MSG_ID, conversationId);
                entity.put(PARAM_USER_ID, userId);
                entity.put(PARAM_LATITUDE, String.format(Locale.US, "%.6f", latitude));
                entity.put(PARAM_LONGITUDE, String.format(Locale.US, "%.6f", longitude));
                entity.put(EMOJI_ID, emoji_id);
                //Log.d(TAG, "[SENT] " + entity.toString());
                connection.addStringEntity(entity.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                status_message = e.getMessage();
            }

            if (connection.execute()) {
                String result = connection.getResult();
                //Log.d(TAG, "[REPLY] " + result);

                try {
                    JSONObject jsonResult = new JSONObject(result);
                    status_code = jsonResult.getInt(PARAM_STATUS);

                    if (jsonResult.has(PARAM_MESSAGE)) {
                        status_message = jsonResult.getString(PARAM_MESSAGE);
                    } else {
                        status_message = "";
                    }

                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    status_message = e.getMessage();
                }
            } else {
                status_message = CONNECTION_ERROR;
            }

            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return runNow();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            onReplyMessageResult(success, status_code, status_message);
        }
    }

    public static class GetAllMessagesAsyncTask extends ParallelAsyncTask {

        private final String conversationId;

        public final List<ReplyMsg> reply_msgs;

        public int status_code;
        public String status_message;
        public String conversation_topic;
        public long conversation_timestamp;
        public String emotion_id;
        private int counts;

        public GetAllMessagesAsyncTask(String cid) {
            this.conversationId = cid;
            this.reply_msgs = new ArrayList<>();
        }

        public void onGetAllMessagesResult(boolean success, int status_code, String status_message, List<ReplyMsg> replies) {
        }

        @Override
        public boolean runNow() {
            TTHttpConnection connection = getTTHttpConnection(CHAT_SERVICE_GET_ALL_REPLY_MESSAGE);

            try {
                JSONObject entity = new JSONObject();
                entity.put(PARAM_SERVER_MESSAGE_ID, conversationId);
                //Log.d(TAG, "[SENT] " + entity.toString());
                connection.addStringEntity(entity.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                status_message = e.getMessage();
            }

            if (connection.execute()) {
                String result = connection.getResult();

                try {
                    JSONObject jsonResult = new JSONObject(result);
                    status_code = jsonResult.getInt(PARAM_STATUS);

                    if (jsonResult.has(PARAM_MESSAGE)) {
                        status_message = jsonResult.getString(PARAM_MESSAGE);
                    } else {
                        status_message = "";
                    }

                    counts = jsonResult.getInt(PARAM_COUNTS);

                    conversation_topic = jsonResult.getString(PARAM_MESSAGE_TOPIC);
                    try {
                        conversation_timestamp = Long.parseLong(jsonResult.getString(PARAM_TIMESTAMP));
                    } catch (NumberFormatException e) {
                        conversation_timestamp = 0L;
                    }

                    JSONArray jsonReplyList = jsonResult.getJSONArray(PARAM_MESSAGE_REPLY_LIST);
                    if (counts == jsonReplyList.length()) {
                        for (int i = 0; i < counts; ++i) {
                            JSONObject jsonReply = jsonReplyList.getJSONObject(i);
                            String rid = jsonReply.getString(PARAM_SERVER_MSG_REPLY_ID);
                            String replyMsg = jsonReply.getString(PARAM_REPLY_MSG);
                            String uid = jsonReply.getString(PARAM_USER_ID);
                            String emojiId = jsonReply.getString(EMOJI_ID);
                            long timestamp;
                            try {
                                timestamp = Long.parseLong(jsonReply.getString(PARAM_TIMESTAMP));
                            } catch (NumberFormatException e) {
                                timestamp = 0L;
                            }

                            reply_msgs.add(new ReplyMsg(rid, uid, replyMsg, timestamp, emojiId));
                        }
                    }

                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    status_message = e.getMessage();
                }
            } else {
                status_message = CONNECTION_ERROR;
            }

            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return runNow();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            onGetAllMessagesResult(success, status_code, status_message, reply_msgs);
        }

        public final static class ReplyMsg {

            public final String replyId;
            public final String userId;
            public final String message;
            public final long timestamp;
            public final String emotion_id;

            public ReplyMsg(String rid, String uid, String msg, long timestamp,String emotion_id) {
                this.replyId = rid;
                this.userId = uid;
                this.message = msg;
                this.timestamp = timestamp;
                this.emotion_id = emotion_id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ReplyMsg replyMsg = (ReplyMsg) o;

                if (timestamp != replyMsg.timestamp) return false;
                if (replyId != null ? !replyId.equals(replyMsg.replyId) : replyMsg.replyId != null)
                    return false;
                if (userId != null ? !userId.equals(replyMsg.userId) : replyMsg.userId != null)
                    return false;
                return !(message != null ? !message.equals(replyMsg.message) : replyMsg.message != null);

            }

            @Override
            public int hashCode() {
                int result = replyId != null ? replyId.hashCode() : 0;
                result = 31 * result + (userId != null ? userId.hashCode() : 0);
                result = 31 * result + (message != null ? message.hashCode() : 0);
                result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
                return result;
            }

            @Override
            public String toString() {
                return "ReplyMsg{" +
                        "replyId='" + replyId + '\'' +
                        ", userId='" + userId + '\'' +
                        ", message='" + message + '\'' +
                        ", timestamp=" + timestamp +
                        ", emotion_id='" + emotion_id + '\'' +
                        '}';
            }
        }
    }

    public static class GetPreviousMessagesAsyncTask extends ParallelAsyncTask {

        private final String userId;
        private final double latitude;
        private final double longitude;
        private final Integer max_posts;
        private final Integer max_hours;

        public final List<String> server_msg_id;

        public int status_code;
        public String status_message;

        public GetPreviousMessagesAsyncTask(String userId, double latitude, double longitude, Integer max_posts, Integer max_hours) {
            this.userId = userId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.max_posts = max_posts;
            this.max_hours = max_hours;

            this.server_msg_id = new ArrayList<>();
        }

        public void onGetPreviousMessagesResult(boolean success, int status_code, String status_message, List<String> server_msg_id) {
        }

        @Override
        public boolean runNow() {
            TTHttpConnection connection = getTTHttpConnection(CHAT_SERVICE_GET_PREVIOUS_MESSAGES);

            try {
                JSONObject entity = new JSONObject();
                entity.put(PARAM_USER_ID, userId);
                entity.put(PARAM_LATITUDE, String.format(Locale.US, "%.6f", latitude));
                entity.put(PARAM_LONGITUDE, String.format(Locale.US, "%.6f", longitude));

                if (max_posts != null) {
                    entity.put(PARAM_MAX_POSTS, max_posts);
                }

                if (max_hours != null) {
                    entity.put(PARAM_MAX_HOURS, max_hours);
                }

                //Log.d(TAG, "[SENT] " + entity.toString());
                connection.addStringEntity(entity.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                status_message = e.getMessage();
            }

            if (connection.execute()) {
                String result = connection.getResult();
                //Log.d(TAG, "[REPLY] " + result);

                try {
                    JSONObject jsonResult = new JSONObject(result);
                    status_code = jsonResult.getInt(PARAM_STATUS);

                    if (jsonResult.has(PARAM_MESSAGE)) {
                        status_message = jsonResult.getString(PARAM_MESSAGE);
                    } else {
                        status_message = "";
                    }

                    JSONArray serverMsgIds = jsonResult.getJSONArray("server_msg_id");
                    int count = serverMsgIds.length();
                    for (int i = 0; i < count; ++i) {
                        server_msg_id.add(serverMsgIds.getString(i));
                    }

                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    status_message = e.getMessage();
                }
            } else {
                status_message = CONNECTION_ERROR;
            }

            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return runNow();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            onGetPreviousMessagesResult(success, status_code, status_message, server_msg_id);
        }
    }


    public static class GetNewsAsyncTask extends ParallelAsyncTask {
        private int status_code;
        private String status_message;
        private NewsResult newsResult;
        private NewsFragment.NewsResults newsResults;

        private void onGetNewsAsyncTaskResult(Boolean success){}

        public GetNewsAsyncTask(NewsFragment.NewsResults newsResults) {
            this.newsResults = newsResults;
        }

        @Override
        public boolean runNow() {

            TTHttpConnection connection = getTTHttpConnection(CHAT_SERVICE_GET_NEWS);
            if (connection.execute()) {
                String result = connection.getResult();
                //Log.d(TAG, "[REPLY] " + result);
                ArrayList<NewsItem> news_list = new ArrayList<>();
                try {
                    JSONObject jsonResult = new JSONObject(result);
                    status_code = jsonResult.getInt(PARAM_STATUS);

                    if (jsonResult.has(PARAM_MESSAGE)) {
                        status_message = jsonResult.getString(PARAM_MESSAGE);
                    } else {
                        status_message = "";
                    }

                    JSONArray news = jsonResult.getJSONArray("news_list");
                    int count = news.length();
                    for (int i = 0; i < count; ++i) {
                        if (i != 0 && i % 3 == 0) {
                            news_list.add(i, new NewsItem(null, null, null, true));
                            continue;
                        }
                        JSONObject jsonItem = new JSONObject(news.get(i).toString());
                        String title = jsonItem.getString(TITLE);
                        String posted_date = jsonItem.getString(POSTED_DATE);
                        String link = jsonItem.getString(LINK);

                        news_list.add(new NewsItem(title, posted_date, link, false));
                    }

                    newsResult = new NewsResult(null, news_list);
                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    status_message = e.getMessage();
                    newsResult = new NewsResult(status_message, null);
                }
            } else {
                status_message = CONNECTION_ERROR;
                newsResult = new NewsResult(status_message, null);
            }
            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return runNow();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            onGetNewsAsyncTaskResult(success);
            newsResults.onNewsResults(newsResult);
        }


        public static final class NewsResult {
            private String message;
            private List<NewsItem> items;

            public NewsResult(String message, List<NewsItem> items) {
                this.message = message;
                this.items = items;
            }

            public String getMessage() {
                return message;
            }

            public List<NewsItem> getItems() {
                return items;
            }
        }
        public static final class NewsItem {
            private String title;
            private String posted_date;
            private String link;
            private boolean isAdverb;

            public NewsItem(String title, String posted_date, String link, boolean isAdverb) {
                this.title = title;
                this.posted_date = posted_date;
                this.link = link;
                this.isAdverb = isAdverb;
            }

            public String getTitle() {
                return title;
            }

            public String getPosted_date() {
                return posted_date;
            }

            public String getLink() {
                return link;
            }

            public boolean isAdverb() {
                return isAdverb;
            }
        }
    }

    private static TTHttpConnection getTTHttpConnection(String service) {
        TTHttpConnection connection = new TTHttpConnection();
        //connection.addURL(CHAT_SERVER_PRIMARY + CHAT_SERVER_PATH + service);
        connection.addURL(CHAT_SERVER_SECONDARY + CHAT_SERVER_PATH + service);
        connection.setTimeouts(TIMEOUT_CONNECT, TIMEOUT_READ);
        connection.setIsPostRequestMethod(true);
        connection.setIsResultWanted(true);
        connection.addProperty(PROPERTY_CONTENT_TYPE, PROPERTY_CONTENT_TYPE_APPLICATION_JSON);

        return connection;
    }

    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_SERVER_MSG_ID = "server_msg_id";
    private static final String EXTRA_SERVER_REPLY_MSG_ID = "server_reply_msg_id";
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_TIMESTAMP = "timestamp";

    public static boolean processNewConversation(ContentResolver contentResolver, Map<String, String> data) {
        String cid = data.get(EXTRA_SERVER_MSG_ID); // CID = Conversation ID (thread id)
        String message = data.get(EXTRA_MESSAGE);
        int emoji_id = Integer.parseInt(data.get(EMOJI_ID));

        long timestamp;
        try {
            timestamp = Long.parseLong(data.get(EXTRA_TIMESTAMP));
        } catch (NumberFormatException e) {
            timestamp = 0L;
        }

        ContentValues values = new ContentValues(4);
        values.put(ConversationsContract.Conversation._CID, cid);
        values.put(ConversationsContract.Conversation._MESSAGE, message);
        values.put(ConversationsContract.Conversation._TIMESTAMP, timestamp);
        values.put(ConversationsContract.Conversation._EMOJI_ID, emoji_id);
        //Log.d(TAG, String.format("[GCM] {\"timestamp\":\"%d\",\"cid\":\"%s\",\"message\":\"%s\"}", timestamp, cid, message));

        return contentResolver.insert(ConversationsContract.Conversation.CONTENT_URI, values) != null;
    }

    public static boolean processConversationReply(ContentResolver contentResolver, Map<String, String> data) {
        String cid = data.get(EXTRA_SERVER_MSG_ID);
        String rid = data.get(EXTRA_SERVER_REPLY_MSG_ID); // RID = Reply ID
        String uid = data.get(EXTRA_USER_ID); // UID = User ID
        String message = data.get(EXTRA_MESSAGE);
        String emoji_id = data.get(EMOJI_ID);//idAvatar
        long timestamp;
        try {
            timestamp = Long.parseLong(data.get(EXTRA_TIMESTAMP));
        } catch (NumberFormatException e) {
            timestamp = 0L;
        }

        ContentValues values = new ContentValues(6);
        values.put(ConversationsContract.Conversation._CID, cid);
        values.put(ConversationsContract.Conversation._RID, rid);
        values.put(ConversationsContract.Conversation._UID, uid);
        values.put(ConversationsContract.Conversation._MESSAGE, message);
        values.put(ConversationsContract.Conversation._TIMESTAMP, timestamp);
        values.put(ConversationsContract.Conversation._EMOJI_ID, emoji_id);
        //Log.d(TAG, String.format("[GCM] {\"timestamp\":\"%d\",\"cid\":\"%s\",\"rid\":\"%s\",\"uid\":\"%s\",\"message\":\"%s\"}", timestamp, cid, rid, uid, message));

        return contentResolver.insert(ConversationsContract.Conversation.CONTENT_URI, values) != null;
    }
}
