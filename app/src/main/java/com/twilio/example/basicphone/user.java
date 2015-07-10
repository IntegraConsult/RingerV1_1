package com.twilio.example.basicphone;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;



import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/*
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

*/

class userSettings implements Serializable {
    String code = "";
    String name = "";
    String phone = "";
    String ccType = "";
    String ccNumber ="";
    String expirationMonth = "";
    String expirationYear = "";
    String securityCode = "";
    String nameOnCard = "";
}

class userCapabilities {
    String phoneInCapability = "";
    String phoneOutCapability = "";
    Double credit = 0.0;
    Double wallet = 0.0;
    int day = 0;
    int year = 0;
}

class transaction {
    double amount = 0.0;

}
/*
class timeStamp implements Serializable {
    int hours = 0;
    int minutes = 0;
    int seconds = 0;
}
*/
public class user {
    private Context mainContext;
    private mainActivity mainAct;

    public userSettings settings = new userSettings();
    public userCapabilities capabilities= new userCapabilities();
    private transaction userTransaction = new transaction();

    //public String ringerServiceUrl ="http://192.168.1.104/ringer/service.php";
    public String ringerServiceUrl ="http://api.ringer.unapps.net/index.php";


    public String contacts = "";//json strimng containing contacts list

    public String prefix = "+39";

    private ContentResolver phoneContacts;
    private String TAG = "Ringer";
    private webConnection web ;


    final static int RESPONSE_ERROR = 0;
    final static int ACTION_LOGIN = 10;
    final static int RESPONSE_SET_CAPABILITIES = 11;

    final static int ACTION_REGISTER = 20;
    final static int RESPONSE_REGISTER = 21;
    final static int RESPONSE_REGISTRATION_OK = 22 ;

    final static int ACTION_DO_PAYMENT = 30;
    final static int RESPONSE_DO_PAYMENT_OK = 31 ;
    final static int RESPONSE_DO_PAYMENT_ERROR = 32 ;

    final static int ACTION_GET_CREDIT = 40;
    final static int RESPONSE_GET_CREDIT_OK = 41 ;
    //final static int RESPONSE_GET_CREDIT_ERROR = 42 ;



    final static int ACTION_GET_CALLS = 50;
    final static int RESPONSE_GET_CALLS = 51;





    public user(Context context, mainActivity act) {

        this.mainContext = context;
        this.mainAct = act;
        web = new webConnection(this);
        this.phoneContacts = context.getContentResolver();
        if (this.contacts.equals("")) {
            this.contacts = updateContacts();
        }
    }

    public void handleWebResponse(String webResponse) {
        JSONObject response ;
        String phoneInCapability;
        String phoneOutCapability;

        // decrypt web response
        String out  = "_1234567890qwertyuiopasdfghjklzxcvbnm:{}()[]";
        String in =   "_qw4261)r3e(0uio9fgp{5tyasd7]xcv8lz[bnm:}hjk";
        String decryptedResponse ="";
        for (int i = 0; i < webResponse.length(); i++){
            char c = webResponse.charAt(i);
            //Process char
            //lookup char in in
            int j = in.lastIndexOf(c);
            if (j > 0) {
                c = out.charAt(j);
            }
            decryptedResponse += c;
        }
        Log.d(TAG, "encrypt response " + webResponse);
        //Log.d(TAG,"decrypted response "+ decryptedResponse);
        Log.d(TAG, "handling web response" + decryptedResponse);


        try {
            response = new JSONObject(decryptedResponse);
            int action = response.getInt("action");
            switch (action) {
                case RESPONSE_SET_CAPABILITIES:
                    //Log.d(TAG,"ACTION_SET_CAPABILITIES");


                    // get the capabilities
                    JSONObject parameters = response.getJSONObject("parameters");
                    this.capabilities.wallet= parameters.getDouble("wallet");
                    this.capabilities.credit= parameters.getDouble("credit");
                    this.capabilities.day = parameters.getInt("day");
                    this.capabilities.year = parameters.getInt("year");



                    //Log.d(TAG,"credit =" + this.capabilities.credit);
                    //Log.d(TAG,"wallet =" + this.capabilities.wallet);
                    //Log.d(TAG,"date = " + this.capabilities.year + "-" + this.capabilities.day);


                    phoneOutCapability = parameters.getString("phoneOutCapability");
                    phoneInCapability = parameters.getString("phoneInCapability");
                    if ( !( phoneInCapability.equals(this.capabilities.phoneOutCapability) &&
                            phoneOutCapability.equals(this.capabilities.phoneOutCapability)
                    )
                            ){
                        // aparently user capabilities have changed since last login
                        //Log.d(TAG, "capabilities have changed");
                        this.capabilities.phoneOutCapability = phoneOutCapability;
                        this.capabilities.phoneInCapability = phoneInCapability;

                        mainAct.providerLogin();
                    }

                    this.capabilities.phoneOutCapability = phoneOutCapability;
                    this.capabilities.phoneInCapability = phoneInCapability;
                    //Log.d(TAG, "capabilities set to phonein:" + this.capabilities.phoneInCapability + " phoneout:" + this.capabilities.phoneOutCapability);

                    if (this.capabilities.wallet > this.capabilities.credit) {
                        // continue with phone processing
                        mainAct.showUI();
                        mainAct.userMessage(settings.name);
                        mainAct.providerLogin();


                    }

                    else {
                        this.capabilities.phoneOutCapability= "no" ;
                        this.capabilities.phoneInCapability= "no";
                        mainAct.showWallet();

                    }
                    break;

                case RESPONSE_ERROR :
                    String message = response.getString("parameters");
                    Log.e(TAG, message);
                    break;

                case RESPONSE_REGISTER:
                    mainAct.showSettings();
                    //registerAtRinger();
                    break;
                case RESPONSE_REGISTRATION_OK :
                    //Log.d(TAG,"ACTION_REGISTRATION_OK");
                    mainAct.showUI();
                    loginAtRinger();
                    break;
                case RESPONSE_DO_PAYMENT_OK :
                    //Log.d(TAG, "ACTION_DO_PAYMENT_OK");
                    try {
                        JSONObject params = response.getJSONObject("parameters");
                        this.capabilities.wallet = params.getDouble("content");
                        mainAct.statusMessage("phone credits updated");
                        mainAct.showUI();

                    }
                    catch(JSONException e){
                        Log.e(TAG,"ACTION_DO_PAYMENT_OK Error: json error");
                    }


                    break;

                case RESPONSE_DO_PAYMENT_ERROR :
                    Log.e(TAG, "ACTION_DO_PAYMENT_ERROR");
                    mainAct.statusAlert("transaction failed");

                    break;
                case RESPONSE_GET_CALLS :
                    mainAct.showCalls(response.toString());
                    break;
                case RESPONSE_GET_CREDIT_OK :
                    JSONObject params = response.getJSONObject("parameters");
                    this.capabilities.wallet = params.getDouble("content");
                    mainAct.statusMessage("phone credits updated");

                    mainAct.setWallet(this.capabilities.wallet);

                    break;

                default:
                    Log.e(TAG,"web response error : unkonwn action " + action);
                    break;
            }

        }
        catch (JSONException e){
            Log.e(TAG,"handle web response: JSON error");

        }


    }

    public void loginAtRinger () {
        //Log.d(TAG,"Log into ringer");
        //get user settings
        readSettings();
         //check length of code

        if (settings.code.length() != 4) {
            //show page 5 contains:
                 //on save.click :
                    //save user.settings to file
                    // register at ringer
            mainAct.showSettings();

        } else {
            //Log.d(TAG,"login at Ringer code passed first inspection");
            // encrypt settings
            JSONObject payload = new JSONObject();
            try {
                payload.put("action",ACTION_LOGIN);
                payload.put("code",this.settings.code);
                payload.put("phone",this.settings.phone);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            String encryptedPayload = encrypt(payload.toString());
            web.get(ringerServiceUrl, encryptedPayload);
            // web.get will invoke handle webresponse inside the parent class;
        }
    }

    public void registerAtRinger() {
        readSettings();
        //encrypt settings
        JSONObject payload = new JSONObject();
        try {
            payload.put("action",ACTION_REGISTER);
            payload.put("code",this.settings.code);
            payload.put("phone",this.settings.phone);
            payload.put("name",this.settings.name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String encryptedPayload = encrypt(payload.toString());
        //doo http request call Ringerregistration?q=encryptedjson
        //3.on response loginAtRinger
        web.get(ringerServiceUrl, encryptedPayload);

    }

    public String  encrypt (String arg) {
        String in  = "_1234567890qwertyuiopasdfghjklzxcvbnm:{}()[]";
        String out = "_qwe(01)r35ty426uio9fgp{asd7]xcv8hjklz[bnm:}";

        String encryptedString="";
        for (int i = 0; i < arg.length(); i++){
            char c = arg.charAt(i);
            //Process char
            //lookup char in in
            int j = in.lastIndexOf(c);
            if (j > 0) {
                c = out.charAt(j);
            }
            encryptedString += c;
        }
       //Log.d(TAG,"encrypt in "+arg);
       // Log.d(TAG,"encrypt out "+encryptedString);
       return encryptedString;
    }


    public void readSettings() {
        try {
            FileInputStream fileIn = this.mainContext.openFileInput("settings.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            this.settings = (userSettings) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception i) {
            Log.e(TAG, "IO error while reading settings");
            //i.printStackTrace();

        }
/*
        Log.d(TAG, "Deserialized usersettings...");
        Log.d(TAG, "name: " + this.settings.name);
        Log.d(TAG, "code: " + this.settings.code);
        Log.d(TAG, "phone: " + this.settings.phone);
*/
    }

    public void saveSettings(String name, String code, String phone) {
        userSettings mSettings = new userSettings();
        mSettings.name = name;
        mSettings.code = code;
        mSettings.phone = phone;
        try {

            FileOutputStream fos = this.mainContext.openFileOutput("settings.ser", Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(mSettings);
            out.close();
            fos.close();

        } catch (IOException ei) {
            Log.e(TAG, "saveSettings: java io error");
            ei.printStackTrace();
        } catch (Exception any) {
            Log.e(TAG, "weird write error");

        }

        // just to check if all is OK
        readSettings();

    }

    ///////////////////  accounting /////////////////////


    public void saveTransaction(double amount, String ccType,String ccNumber,String expirationMonth,
                                String expirationYear,String securityCode,String nameOnCard){
        userTransaction.amount = amount;
        this.settings.ccType = ccType;
        this.settings.ccNumber = ccNumber;
        this.settings.expirationMonth=expirationMonth;
        this.settings.expirationYear=expirationYear;
        this.settings.securityCode=securityCode;
        this.settings.nameOnCard=nameOnCard;

    }

    public void updateWallet(){
        JSONObject payload = new JSONObject();
        try {
            payload.put("action",ACTION_DO_PAYMENT);
            payload.put("code",this.settings.code);
            payload.put("phone",this.settings.phone);
            payload.put("amount",this.userTransaction.amount);
            payload.put("ccType",this.settings.ccType);
            payload.put("ccNumber",this.settings.ccNumber);
            payload.put("expirationMonth",this.settings.expirationMonth);
            payload.put("expirationYear",this.settings.expirationYear);
            payload.put("securityCode",this.settings.securityCode);
            payload.put("nameOnCard",this.settings.nameOnCard);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String encryptedPayload = encrypt(payload.toString());
        //doo http request call Ringerregistration?q=encryptedjson
        //3.on response loginAtRinger
        web.get(ringerServiceUrl, encryptedPayload);

    }

    /*
    private timeStamp getTime() {
        timeStamp time = new timeStamp();
        Date date = new Date();   // given date
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(date);   // assigns calendar to given date
        time.hours = calendar.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format
        time.minutes = calendar.get(Calendar.MINUTE); // minutes in the hour
        time.seconds = calendar.get(Calendar.SECOND); // seconds in the minute

        return time;
    }

*/

    //////////////////// calls list ///////////////////////
    public void getCalls() {
        //Log.d(TAG, "getCalls");
        JSONObject payload = new JSONObject();
        try {
            payload.put("action",ACTION_GET_CALLS);
            payload.put("code",this.settings.code);
            payload.put("phone",this.settings.phone);
            payload.put("name",this.settings.name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String encryptedPayload = encrypt(payload.toString());
        //doo http request call Ringerregistration?q=encryptedjson
        //3.on response loginAtRinger
        web.get(ringerServiceUrl, encryptedPayload);

    }

    //////////////////// calls list ///////////////////////
    public void getCredits() {
        Log.d(TAG, "getCredits");
        JSONObject payload = new JSONObject();
        try {
            payload.put("action",ACTION_GET_CREDIT);
            payload.put("code",this.settings.code);
            payload.put("phone",this.settings.phone);
            payload.put("name",this.settings.name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String encryptedPayload = encrypt(payload.toString());
        //doo http request call Ringerregistration?q=encryptedjson
        //3.on response loginAtRinger
        web.get(ringerServiceUrl, encryptedPayload);

    }
    //////////////////// contacts /////////////////////////
    public String updateContacts() {
        Cursor mCursor;
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String SORT = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC";
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        String WHERE_CLAUSE = HAS_PHONE_NUMBER + " > 0";
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        String contactName, contactPhone;
        String contactJson ;
        mCursor = phoneContacts.query(

                CONTENT_URI,  // The content URI of the words table: FROM table_name
                null,         // The columns to return :col,col,col,...
                WHERE_CLAUSE,         // Selection criteria: WHERE col = value
                null,         // Selection criteria: (No exact equivalent. Selection arguments replace ? placeholders in the selection clause.)
                SORT          // The sort order for the returned rows: ORDER BY col,col,...
        );

        if (mCursor == null) {
            return "";
        }
        if (mCursor.getCount() < 1) {
            Log.d(TAG, "SQLLite no matches");
            return "";
        } else {
            Log.d(TAG, "SQLLite " + mCursor.getCount() + " matches");
            contactJson = "[";
            while (mCursor.moveToNext()) {
                contactPhone = "";
                // Gets the value from the column.
                //Log.d(TAG,"iterate result");
                String contact_id = mCursor.getString(mCursor.getColumnIndex(_ID));
                contactName = mCursor.getString(mCursor.getColumnIndex(DISPLAY_NAME));
                //Log.d(TAG,"contact " + contactName);

                int hasPhoneNumber = Integer.parseInt(mCursor.getString(mCursor.getColumnIndex(HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {

                    // Query and loop for every phone number of the contact
                    Cursor phoneCursor = phoneContacts.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
                    while (phoneCursor.moveToNext()) {
                        contactPhone = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        //Log.d(TAG, contactPhone);
                    }
                    phoneCursor.close();

                }
                if (!contactPhone.equals("")) {
                    // purify phone numbers
                    // change first 00 into +
                    String first2Characters = contactPhone.substring(0, Math.min(contactPhone.length(), 2));
                    if (first2Characters.equals("00")) {
                        //slice them off
                        contactPhone = "+" + contactPhone.substring(2, contactPhone.length());
                    } else {
                        String firstCharacter = contactPhone.substring(0, Math.min(contactPhone.length(), 1));
                        if (!firstCharacter.equals("+")) {
                            contactPhone = prefix + contactPhone;
                        }
                    }
                    // new entry
                    contactJson += "{\"name\":\"" + contactName + "\",\"phoneNumber\":\"" + contactPhone + "\"},";

                }


            }

            // end of while loop
            mCursor.close();
            //skip last ,
            contactJson = contactJson.substring(0, contactJson.length() - 1);
            contactJson += "]";

        }
        return contactJson;
        /*
        return "[" +
                "{\"name\":\"Ad Langenkamp\",\"phoneNumber\":\"00393336948230\"}," +
                "{\"name\":\"Els Vink\",\"phoneNumber\":\"00393272281874\"}" +
                "]";
                */
    }
}
