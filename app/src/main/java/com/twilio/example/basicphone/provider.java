/*
 *  Copyright (c) 2011 by Twilio, Inc., all rights reserved.
 *
 *  Use of this software is subject to the terms and conditions of 
 *  the Twilio Terms of Service located at http://www.twilio.com/legal/tos
 */

package com.twilio.example.basicphone;

import java.util.Map;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.twilio.client.Connection;
import com.twilio.client.ConnectionListener;
import com.twilio.client.Device;
import com.twilio.client.Device.Capability;
import com.twilio.client.DeviceListener;
import com.twilio.client.PresenceEvent;
import com.twilio.client.Twilio;

import org.json.JSONException;
import org.json.JSONObject;

public class provider implements DeviceListener,
                                   ConnectionListener
{
    private static final String TAG = "Ringer";

    // TODO: change this to point to the script on your public server
    private static final String AUTH_PHP_SCRIPT = "https://mymobilephone.herokuapp.com/token";
    //private static final String AUTH_PHP_SCRIPT = "http://api.ringer.unapps.net/twilio/token.php";
    private static final int AUTHORIZATION_ERROR_CODE_JWT_TOKEN_EXPIRED = 31205;// acces token expired
    private static final int AUTHORIZATION_ERROR_CODE_GENERIC = 31201;// generic authorization error
    private static final int GENERIC_ERROR = 31000; // generic general error
    // apparently error are thrown in the following order
      // 05:35:09.836 error 31201
      // 06:12:46.926 error 31205
      // 06:13:09.711 error 31000


    public interface LoginListener
    {
        public void onLoginStarted();
        public void onLoginFinished();
        public void onLoginError(Exception error);
    }

    public interface BasicConnectionListener
    {
        public void onIncomingConnectionDisconnected();
        public void onConnectionConnecting();
        public void onConnectionConnected();
        public void onConnectionFailedConnecting(Exception error);
        public void onConnectionDisconnecting();
        public void onConnectionDisconnected();
        public void onConnectionFailed(Exception error);
    }

    public interface BasicDeviceListener
    {
        public void onDeviceStartedListening();
        public void onDeviceStoppedListening(Exception error);
    }

    private static provider instance;
    public static final provider getInstance(Context context)
    {
        if (instance == null)
            instance = new provider(context);
        return instance;
    }

    private final Context context;
    private mainActivity mainAct;
    private LoginListener loginListener;
    private BasicConnectionListener basicConnectionListener;
    private BasicDeviceListener basicDeviceListener;

    private static boolean twilioSdkInited;
    private static boolean twilioSdkInitInProgress;
    private boolean queuedConnect;

    private Device device;
    private Connection pendingIncomingConnection;
    private Connection connection;
    private boolean speakerEnabled;
    
    private String lastClientName;
    private boolean lastAllowOutgoing;
    private boolean lastAllowIncoming;

    public provider(Context context)
    {

        this.context = context;

    }
    public void setParent (mainActivity act) {
        this.mainAct = act;
    }

    public void setListeners(LoginListener loginListener,
                             BasicConnectionListener basicConnectionListener,
                             BasicDeviceListener basicDeviceListener)
    {
        this.loginListener = loginListener;
        this.basicConnectionListener = basicConnectionListener;
        this.basicDeviceListener = basicDeviceListener;
    }

    private void obtainCapabilityToken(String clientName, 
    								  boolean allowOutgoing, 
    								  boolean allowIncoming)
    {
    	StringBuilder url = new StringBuilder();
    	url.append(AUTH_PHP_SCRIPT);
        url.append("?allowOutgoing=").append(allowOutgoing);
        if (allowIncoming && (clientName != null)) {
            url.append("&&client=").append(clientName);
        }
        // This runs asynchronously!
        new GetAuthTokenAsyncTask().execute(url.toString());


        String code = mainAct.phoneUser.settings.code;
        String phone = mainAct.phoneUser.settings.phone;
        JSONObject request = new JSONObject();
        try {
            request.put("code",code);
            request.put("phone",phone);
        }
        catch (JSONException e) {
            Log.e(TAG,"obtainCapabilityToken: JSON encryption error");
        }
        String encryptedRequest = this.mainAct.phoneUser.encrypt(request.toString()) ;


        String mUrl = AUTH_PHP_SCRIPT + "?q=" +  encryptedRequest;

        // This runs asynchronously!
        //new GetAuthTokenAsyncTask().execute(mUrl);






    }

    private boolean isCapabilityTokenValid()
    {
        if (device == null || device.getCapabilities() == null)
            return false;
        long expTime = (Long)device.getCapabilities().get(Capability.EXPIRATION);
        return expTime - System.currentTimeMillis() / 1000 > 0;
    }

    private void updateAudioRoute()
    {
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(speakerEnabled);
    }

    public void login(final String clientName, 
			  		  final boolean allowOutgoing, 
			  		  final boolean allowIncoming)
    {
        if (loginListener != null)
            loginListener.onLoginStarted();
        
        this.lastClientName = clientName;
        this.lastAllowOutgoing = allowOutgoing;
        this.lastAllowIncoming = allowIncoming;

        if (!twilioSdkInited) {
            if (twilioSdkInitInProgress)
                return;

            twilioSdkInitInProgress = true;
            Twilio.setLogLevel(Log.DEBUG);
            
            Twilio.initialize(context, new Twilio.InitListener()
            {
                @Override
                public void onInitialized()
                {
                    twilioSdkInited = true;
                    twilioSdkInitInProgress = false;
                    obtainCapabilityToken(clientName, allowOutgoing, allowIncoming);
                }

                @Override
                public void onError(Exception error)
                {
                    twilioSdkInitInProgress = false;
                    if (loginListener != null)
                        loginListener.onLoginError(error);
                }
            });
        } else {
        	obtainCapabilityToken(clientName, allowOutgoing, allowIncoming);
        }
    }

    private void reallyLogin(final String capabilityToken)
    {
        try {
            if (device == null) {
                device = Twilio.createDevice(capabilityToken, this);
                Intent intent = new Intent(context, mainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                device.setIncomingIntent(pendingIntent);
            } else
                device.updateCapabilityToken(capabilityToken);

            if (loginListener != null)
                loginListener.onLoginFinished();

            if (queuedConnect) {
                // If someone called connect() before we finished initializing
                // the SDK, let's take care of that here.
                connect(null);
                queuedConnect = false;
            }
        } catch (Exception e) {
            if (device != null)
                device.release();
            device = null;

            if (loginListener != null)
                loginListener.onLoginError(e);
        }
    }

    public void setSpeakerEnabled(boolean speakerEnabled)
    {
        if (speakerEnabled != this.speakerEnabled) {
            this.speakerEnabled = speakerEnabled;
            updateAudioRoute();
        }
    }

    public void connect(Map<String, String> inParams)
    {
        if (twilioSdkInitInProgress) {
            // If someone calls connect() before the SDK is initialized, we'll remember
            // that fact and try to connect later.
            queuedConnect = true;
            return;
        }

        if (!isCapabilityTokenValid())
            login(lastClientName, lastAllowOutgoing, lastAllowIncoming);

        if (device == null)
            return;

        if (canMakeOutgoing()) {
            disconnect();

            connection = device.connect(inParams, this);
            if (connection == null && basicConnectionListener != null)
                basicConnectionListener.onConnectionFailedConnecting(new Exception("Couldn't create new connection"));
        }
    }

    public void disconnect()
    {
        if (connection != null) {
            connection.disconnect();  // will null out in onDisconnected()
            if (basicConnectionListener != null)
                basicConnectionListener.onConnectionDisconnecting();
        }
    }

    public void acceptConnection()
    {
        if (pendingIncomingConnection != null) {
            if (connection != null)
                disconnect();

            pendingIncomingConnection.accept();
            connection = pendingIncomingConnection;
            pendingIncomingConnection = null;
        }
    }

    public void ignoreIncomingConnection()
    {
        if (pendingIncomingConnection != null) {
            pendingIncomingConnection.ignore();
        }
    }

    public boolean isConnected()
    {
        return connection != null && connection.getState() == Connection.State.CONNECTED;
    }

    public Connection.State getConnectionState()
    {
        return connection != null ? connection.getState() : Connection.State.DISCONNECTED;
    }

    public boolean hasPendingConnection()
    {
        return pendingIncomingConnection != null;
    }

    public boolean handleIncomingIntent(Intent intent)
    {
        Device inDevice = intent.getParcelableExtra(Device.EXTRA_DEVICE);
        Connection inConnection = intent.getParcelableExtra(Device.EXTRA_CONNECTION);
        if (inDevice == null && inConnection == null)
            return false;

        intent.removeExtra(Device.EXTRA_DEVICE);
        intent.removeExtra(Device.EXTRA_CONNECTION);

        if (pendingIncomingConnection != null) {
            Log.i(TAG, "A pending connection already exists");
            inConnection.ignore();
            return false;
        }

        pendingIncomingConnection = inConnection;
        pendingIncomingConnection.setConnectionListener(this);

        return true;
    }

    public boolean canMakeOutgoing()
    {
        if (device == null)
            return false;

        Map<Capability, Object> caps = device.getCapabilities();
        return caps.containsKey(Capability.OUTGOING) && (Boolean)caps.get(Capability.OUTGOING);
    }

    public boolean canAcceptIncoming()
    {
        if (device == null)
            return false;

        Map<Capability, Object> caps = device.getCapabilities();
        return caps.containsKey(Capability.INCOMING) && (Boolean)caps.get(Capability.INCOMING);
    }
    
    public void setCallMuted(boolean isMuted) {
    	if (connection != null) {
    		connection.setMuted(isMuted);
    	}
    }

    @Override  /* DeviceListener */
    public void onStartListening(Device inDevice)
    {
        if (basicDeviceListener != null)
            basicDeviceListener.onDeviceStartedListening();
    }

    @Override  /* DeviceListener */
    public void onStopListening(Device inDevice)
    {
        if (basicDeviceListener != null)
            basicDeviceListener.onDeviceStoppedListening(null);
    }

    @Override  /* DeviceListener */
    public void onStopListening(Device inDevice, int inErrorCode, String inErrorMessage)
    {   Log.d(TAG,"in Provider onStopListening Device, errorcode ="+ inErrorCode);

        // get a new token
        switch (inErrorCode) {
            case AUTHORIZATION_ERROR_CODE_JWT_TOKEN_EXPIRED:
            case AUTHORIZATION_ERROR_CODE_GENERIC:
                Log.d(TAG,"caught authorization error");
                if (WIFIConnected()) {
                    // lets try and login again to obtain a new  correct token
                    if (!isCapabilityTokenValid())
                        login(lastClientName, lastAllowOutgoing, lastAllowIncoming);
                }
                else {
                    // throw the error
                    Log.e(TAG,"no WIFI connection while trying to refresh token");
                    if (basicDeviceListener != null)
                        basicDeviceListener.onDeviceStoppedListening(new Exception(inErrorMessage));

                }

                break;
            case GENERIC_ERROR:

                // lets see if wifi is interrupted
                if (!WIFIConnected()) {
                   Log.d (TAG, "caught generic error while there was no WIFI, lets simply wait");
                    if (!isCapabilityTokenValid())
                        login(lastClientName, lastAllowOutgoing, lastAllowIncoming);
                }
                else {
                    // no clue what has happened: throw an error
                    Log.e (TAG, "Caught 3100 generic error while WIFI was on");
                    if (basicDeviceListener != null)
                        basicDeviceListener.onDeviceStoppedListening(new Exception(inErrorMessage));

                }


                break;
            default:
                Log.d(TAG,"unknown error occured with code " + inErrorCode);
                if (basicDeviceListener != null)
                    basicDeviceListener.onDeviceStoppedListening(new Exception(inErrorMessage));

                break;
        }
    }
    public boolean WIFIConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.mainAct.getSystemService(this.mainAct.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi.isConnected();

    }
    @Override  /* DeviceListener */
    public boolean receivePresenceEvents(Device inDevice)
    {
        return false;
    }

    @Override  /* DeviceListener */
    public void onPresenceChanged(Device inDevice, PresenceEvent inPresenceEvent) { }

    @Override  /* ConnectionListener */
    public void onConnecting(Connection inConnection)
    {
        if (basicConnectionListener != null)
            basicConnectionListener.onConnectionConnecting();
    }

    @Override  /* ConnectionListener */
    public void onConnected(Connection inConnection)
    {
        updateAudioRoute();
        if (basicConnectionListener != null)
            basicConnectionListener.onConnectionConnected();
    }

    @Override  /* ConnectionListener */
    public void onDisconnected(Connection inConnection)
    {
        if (inConnection == connection) {
            connection = null;
            if (basicConnectionListener != null)
                basicConnectionListener.onConnectionDisconnected();
        } else if (inConnection == pendingIncomingConnection) {
            pendingIncomingConnection = null;
            if (basicConnectionListener != null)
                basicConnectionListener.onIncomingConnectionDisconnected();
        }
    }

    @Override  /* ConnectionListener */
    public void onDisconnected(Connection inConnection, int inErrorCode, String inErrorMessage)
    {
        if (inConnection == connection) {
            connection = null;
            if (basicConnectionListener != null)
                basicConnectionListener.onConnectionFailedConnecting(new Exception(inErrorMessage));
        }
    }
    
    
    private class GetAuthTokenAsyncTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			provider.this.reallyLogin(result);
		}

		@Override
		protected String doInBackground(String... params) {
			String capabilityToken = null;
			try {
				capabilityToken = HttpHelper.httpGet(params[0]);;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return capabilityToken;
		}
    }

}
