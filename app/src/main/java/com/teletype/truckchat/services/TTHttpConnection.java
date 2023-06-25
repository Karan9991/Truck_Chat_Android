package com.teletype.truckchat.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public final class TTHttpConnection {

	public interface OnConnectionListener {
		void onConnecting(int index, String url);
		
		void onConnected(int index, String url);
	}
	
	public interface OnResponseListener {
		boolean onResponse(int code, String message);
	}
	
	public interface OnInputStreamListener {
		void onInputStream(InputStream stream) throws IOException;
	}

	private OnConnectionListener mOnConnectionListener;
	private OnResponseListener mOnResponseListener;
	private OnInputStreamListener mOnInputStreamListener;

	private final List<URL> mURLs = new ArrayList<>();
	private final NameValuePairList mQueries = new NameValuePairList();
	private final NameValuePairList mProperties = new NameValuePairList();

	private String mStringEntity;
	private int mTimeoutConnect; // milliseconds
	private int mTimeoutRead; // milliseconds
	private Boolean mIsPostRequestMethod;
	private boolean mIsResultWanted;
	private StringBuilder mResult;

	public TTHttpConnection() {
		setOnConnectionListener(null);
		setOnInputStreamListener(null);
	}
	
	public void setOnConnectionListener(OnConnectionListener listener) {
		if (listener == null) {
			mOnConnectionListener = mDefaultOnConnectionListener;	
		} else {
			mOnConnectionListener = listener;
		}
	}

	public void setOnResponseListener(OnResponseListener listener) {
		mOnResponseListener = listener;
	}
	
	public void setOnInputStreamListener(OnInputStreamListener listener) {
		if (listener == null) {
			mOnInputStreamListener = mDefaultOnInputStreamListener;	
		} else {
			mOnInputStreamListener = listener;
		}
	}

	public boolean addURL(String url) {
		try {
			addURL(new URL(url));
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void addURL(URL url) {
		mURLs.add(url);
	}

	public void addStringEntity(String entity) {
		mStringEntity = entity;
	}

	public void addQuery(String name, String value) {
		mQueries.add(name, value);
	}
	
	public void addProperty(String name, String value) {
		mProperties.add(name, value);
	}

	public void setTimeouts(int connect_timeout_millis, int read_timeout_millis) {
		mTimeoutConnect = connect_timeout_millis;
		mTimeoutRead = read_timeout_millis;
	}
	
	public void setIsPostRequestMethod(boolean isPostMethod) {
		mIsPostRequestMethod = isPostMethod;
	}
	
	public void setIsResultWanted(boolean isWantResult) {
		mIsResultWanted = isWantResult;
	}
	
	public String getResult() {
		return mResult == null ? "" : mResult.toString(); 
	}
	
	public boolean execute() {
		for (int i = 0; i < mURLs.size(); ++i) {
			URL url = mURLs.get(i);

            boolean isSent = false;

			try {
                mOnConnectionListener.onConnecting(i, url.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                try {
                    boolean connected = false;
                    boolean gotResponse = false;

                    connection.setConnectTimeout(mTimeoutConnect);
                    connection.setReadTimeout(mTimeoutRead);

                    for (int j = 0; j < mProperties.size(); ++j) {
                        connection.setRequestProperty(mProperties.getName(j), mProperties.getValue(j));
                    }

                    if (mIsPostRequestMethod != null) {
                        connection.setRequestMethod(mIsPostRequestMethod ? "POST" : "GET");
                    }

                    if (mIsResultWanted) {
                        connection.setDoInput(true);
                    }

                    if (mQueries.size() > 0) {
                        if (mIsPostRequestMethod == null) {
                            connection.setRequestMethod("POST");
                        }

                        connection.setDoOutput(true);

                        OutputStream outputStream = connection.getOutputStream();
                        try {
                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                            try {
                                bufferedWriter.write(mQueries.toQueryString());
                                connected = true;
                                mOnConnectionListener.onConnected(i, url.toString());
                            } finally {
                                bufferedWriter.close();
                            }
                        } finally {
                            outputStream.close();
                        }

                        if (mOnResponseListener != null) {
                            int responseCode = connection.getResponseCode();
                            String responseMessage = connection.getResponseMessage();

                            connected = true;
                            mOnConnectionListener.onConnected(i, url.toString());
                            isSent = true;

                            if (!mOnResponseListener.onResponse(responseCode, responseMessage)) {
                                return false;
                            }
                            gotResponse = true;
                        }
                    } else if (mStringEntity != null) {
						if (mIsPostRequestMethod == null) {
							connection.setRequestMethod("POST");
						}

						connection.setDoOutput(true);

						OutputStream outputStream = connection.getOutputStream();
						try {
							BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
							try {
								bufferedWriter.write(mStringEntity);
							} finally {
								bufferedWriter.close();
							}
						} finally {
							outputStream.close();
						}

						if (mOnResponseListener != null) {
                            int responseCode = connection.getResponseCode();
                            String responseMessage = connection.getResponseMessage();

                            connected = true;
                            mOnConnectionListener.onConnected(i, url.toString());
                            isSent = true;

							if (!mOnResponseListener.onResponse(responseCode, responseMessage)) {
								return false;
							}
							gotResponse = true;
						}
					}

					if (mIsResultWanted) {
                        InputStream inputStream = connection.getInputStream();
                        try {
                            if (!connected) {
                                connected = true;
                                mOnConnectionListener.onConnected(i, url.toString());
                            }

                            isSent = true;

                            if (mOnResponseListener != null && !gotResponse) {
                                if (!mOnResponseListener.onResponse(connection.getResponseCode(), connection.getResponseMessage())) {
                                    return false;
                                }
                            }

                            mOnInputStreamListener.onInputStream(inputStream);
                        } finally {
                            inputStream.close();
                        }
                    }

                    if (!connected) {
                        if (mOnResponseListener != null) {
                            connection.setDoInput(true);
                            InputStream inputStream = connection.getInputStream();
                            mOnConnectionListener.onConnected(i, url.toString());
                            isSent = true;

                            try {
                                if (!mOnResponseListener.onResponse(connection.getResponseCode(), connection.getResponseMessage())) {
                                    return false;
                                }
                            } finally {
                                inputStream.close();
                            }
                        } else {
                            connection.connect();
                            mOnConnectionListener.onConnected(i, url.toString());
                        }
                    }

                    return true;
                } finally {
                    connection.disconnect();
                }
			} catch (IOException e) {
                if (!(e instanceof SocketTimeoutException)) {
                    if (isSent) {
                        return false;
                    }
                }
			}
		}
	
		return false;
	}
	
	private final OnConnectionListener mDefaultOnConnectionListener = new OnConnectionListener() {

		@Override
		public void onConnecting(int index, String url) {
		}

		@Override
		public void onConnected(int index, String url) {
		}
		
	};
	
	private final OnInputStreamListener mDefaultOnInputStreamListener = new OnInputStreamListener() {

		@Override
		public void onInputStream(InputStream stream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream), 65536);
            try {
	            String line;
	            mResult = new StringBuilder();
	            while ((line = reader.readLine()) != null) {
	            	mResult.append(line);
	            }
            } finally {
            	reader.close();
            }
		}
	};

	private class NameValuePairList {

		private final List<NameValuePair> list;

		public NameValuePairList() {
			this(0);
		}

		public NameValuePairList(int capacity) {
			list = new ArrayList<>(capacity);
		}

		public int size() {
			return list.size();
		}

		public void add(String name, String value) {
			list.add(new NameValuePair(name, value));
		}

		public String toQueryString() {
			StringBuilder stringBuilder = new StringBuilder();

			for (NameValuePair pair : list) {
				if (stringBuilder.length() > 0) {
					stringBuilder.append("&");
				}

				try {
					stringBuilder.append(URLEncoder.encode(pair.getName(), "UTF-8"));
					stringBuilder.append("=");
					stringBuilder.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

			return stringBuilder.toString();

		}

		public String getName(int index) {
			return list.get(index).getName();
		}

		public String getValue(int index) {
			return list.get(index).getValue();
		}
	}

	private class NameValuePair {
		private final String name;
		private final String value;

		public NameValuePair(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NameValuePair other = (NameValuePair) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "NameValuePair [name=" + name + ", value=" + value + "]";
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}
}
