package co.poynt.cordova.plugin;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
//import org.apache.cordova.cordova;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.NumberFormat;

import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;

/**
 * Created by dennis on 5/14/16.
 */

public class Poynt extends CordovaPlugin{
    private static final String TAG = "Poynt";
    private static final String LAUNCH_PAYMENT="launchPayment";


    private CallbackContext callbackContext;        // The callback context from which we were invoked.
    private JSONArray executeArgs;

    public static final int UNKNOWN_ERROR = 0;
    public static final int INVALID_ARGUMENT_ERROR = 1;
    public static final int TIMEOUT_ERROR = 2;
    public static final int PENDING_OPERATION_ERROR = 3;
    public static final int IO_ERROR = 4;
    public static final int NOT_SUPPORTED_ERROR = 5;
    public static final int OPERATION_CANCELLED_ERROR = 6;
    public static final int PERMISSION_DENIED_ERROR = 20;

    public static final int COLLECT_PAYMENT_REQUEST = 54321;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action        The action to execute.
     * @param args          JSONArry of arguments for the plugin.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return              A PluginResult object with a status and message.
     */
    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        //TODO add different actions, the one below shoud be "charge"
        this.callbackContext = callbackContext;
        this.executeArgs = args;
        JSONObject arg_object = args.getJSONObject(0);
        if (LAUNCH_PAYMENT.equals(action)) {
            Long amount = arg_object.getLong("amount");
            String referenceId = arg_object.getString("referenceId");
            String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();

            Payment payment = new Payment();
            payment.setReferenceId(referenceId);

            payment.setCurrency(currencyCode);

            payment.setAmount(amount);


            // start Payment activity for result
            try {
                Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
                collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
                this.cordova.startActivityForResult(this, collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
            } catch (ActivityNotFoundException ex) {
                Log.e(TAG, "Poynt Payment Activity not found - did you install PoyntServices?", ex);
                this.callbackContext.error(getErrorString());
            }
        }
        return true;
    }


    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == COLLECT_PAYMENT_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data!=null) {

                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);


                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Type paymentType = new TypeToken<Payment>(){}.getType();
                    String paymentResult = gson.toJson(payment, paymentType);

                    this.callbackContext.success(paymentResult);
                    return;
                }else{
                    //TODO no payment was returned. Notify callback
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                callbackContext.error(getErrorString());
                return;
            }

            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, UNKNOWN_ERROR));
        }
    }

    private String getErrorString(){
        return "{\n" + 
               "    \"status\": \"TRANSACTION_CANCELLED\"\n" +
               "}";
    }
}