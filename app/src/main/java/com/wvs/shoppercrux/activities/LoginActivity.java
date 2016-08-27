package com.wvs.shoppercrux.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.wvs.shoppercrux.R;
import com.wvs.shoppercrux.app.AppConfig;
import com.wvs.shoppercrux.app.AppController;
import com.wvs.shoppercrux.classes.ConnectivityReceiver;
import com.wvs.shoppercrux.helper.SQLiteHandler;
import com.wvs.shoppercrux.helper.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements ConnectivityReceiver.ConnectivityReceiverListener, GoogleApiClient.OnConnectionFailedListener {

    private TextView signUpLink, forgot;
    private EditText email, password;
    private Button login;
    private TextInputLayout emailLabel, passwordLabel;
    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;
    private ScrollView scrollView;
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private SignInButton gPlusSignIn;
    private GoogleSignInOptions gso;
    private Dialog progressDialog;
    private TextView mTextDetails;
    private CallbackManager mCallbackManager;
    private AccessTokenTracker mTokenTracker;
    private ProfileTracker mProfileTracker;
    private FacebookCallback<LoginResult> mFacebookCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            progressDialog = ProgressDialog.show(LoginActivity.this, "", "Logging in...", true);
            Log.d("VIVZ", "onSuccess");
//            AccessToken accessToken = loginResult.getAccessToken();
//            Profile profile = Profile.getCurrentProfile();
//            mTextDetails.setText(constructWelcomeMessage(profile));
            session.setLogin(true);
            fbSignUp();

            //   finish();

        }


        @Override
        public void onCancel() {
            Log.d("VIVZ", "onCancel");
        }

        @Override
        public void onError(FacebookException e) {
            Log.d("VIVZ", "onError " + e);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCallbackManager = CallbackManager.Factory.create();
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        setupTokenTracker();
        setupProfileTracker();
        setupTextDetails();
        setupLoginButton();

        mTokenTracker.startTracking();
        mProfileTracker.startTracking();


        Log.w("Shoppercrux", "onCreate() method excecuted");

        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");

        scrollView = (ScrollView) findViewById(R.id.login);
        gPlusSignIn = (SignInButton) findViewById(R.id.gplus_signin);
        signUpLink = (TextView) findViewById(R.id.link_signup);
        forgot = (TextView) findViewById(R.id.forgot);

        email = (EditText) findViewById(R.id.input_email);
        password = (EditText) findViewById(R.id.input_password);
        login = (Button) findViewById(R.id.btn_login);

        emailLabel = (TextInputLayout) findViewById(R.id.input_email_label);
        passwordLabel = (TextInputLayout) findViewById(R.id.input_password_label);

        email.setTextColor(Color.parseColor("#ffffff"));
        password.setTextColor(Color.parseColor("#ffffff"));
        login.setTextColor(Color.parseColor("#ffffff"));

        emailLabel.setTypeface(font);
        passwordLabel.setTypeface(font);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        signUpLink.setTextColor(Color.parseColor("#ffffff"));
        signUpLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
                finish();
            }
        });

        forgot.setTextColor(Color.parseColor("#ffffff"));
        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ForgotActivity.class));
            }
        });

        // Login button Click Event
        login.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String useremail = email.getText().toString().trim();
                String userpassword = password.getText().toString().trim();

                // Check for empty data in the form
                if (!useremail.isEmpty() && !userpassword.isEmpty()) {
                    // login user
                    checkLogin(useremail, userpassword);
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter the credentials!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        gPlusSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        checkConnection();
    }


    private void fbSignUp() {
        // Log.d(ShopperCrux.TAG, "fb Signup");
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                Log.d("GraphRespone", graphResponse.toString());
                if (jsonObject != null) {
                    String fbEmail = null;
                    String fbName = null;

                    try {
                        fbEmail = jsonObject.getString("email");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        fbName = jsonObject.getString("name");


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(fbEmail != null) {
                        Log.d("email", fbEmail);
                    }
                    Log.d("fbName",fbName);

//                    SharedPreferences.Editor editor = AppController.mUserPreferences.edit();
//                    editor.putString("username", fbEmail);
//                    editor.putString("name", fbName);
//                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("fbEmail",fbEmail);
                    intent.putExtra("fbName",fbName);
                    startActivity(intent);

                    // Log.d("fbName", fbName);
                    //  fbLoginDone(fbEmail, fbName);

//
//                    final Utils utils = Utils.getInstance();
//                    final String fbPass;
//                    if (fbEmail != null) {
//                        fbPass = utils.createPassword(fbEmail);
//                    } else {
//                        fbPass = utils.createPassword(fbName);
//                        fbEmail = fbName;
//                    }

//                    ParseQuery<ParseObject> userQuery = ParseQuery.getQuery(ParseConstants.USER);
//                    userQuery.whereEqualTo(ParseConstants.USER_EMAIL, fbEmail);
//                    final String finalFbEmail = fbEmail;
//                    final String finalFbName = fbName;
//                    userQuery.getFirstInBackground(new GetCallback<ParseObject>() {
//                        @Override
//                        public void done(ParseObject object, ParseException e) {
//                            if (object == null) {
//                                ParseObject user = new ParseObject(ParseConstants.USER);
//                                user.put(ParseConstants.USER_EMAIL, finalFbEmail);
//                                user.put(ParseConstants.USER_USER_NAME, finalFbEmail);
//                                user.put(ParseConstants.USER_PASSWORD, fbPass);
//                                user.put(ParseConstants.USER_NAME, finalFbName);
//                                user.put(ParseConstants.USER_LOGIN_TYPE, "facebook");
//                                user.saveInBackground(new SaveCallback() {
//                                    @Override
//                                    public void done(ParseException e) {
//                                        if (e == null) {
//                                            Log.d(ShopperCrux.TAG, "New User");
//                                            fbLoginDone(finalFbEmail, fbPass, finalFbName);
//                                        } else {
//                                            Log.d(ShopperCrux.TAG, "fbSignUp exception: " + e);
//                                            Toast.makeText(SignInActivity.this,
//                                                    "Sign up Error", Toast.LENGTH_LONG)
//                                                    .show();
//                                        }
//                                    }
//                                });
//                            } else {
//                                Log.d(ShopperCrux.TAG, "User exists");
//                                object.put(ParseConstants.USER_LOGIN_TYPE, "faceboook");
//                                object.saveInBackground();
//                                fbLoginDone(finalFbEmail, fbPass, finalFbName);
//                            }
//                        }
//                    });

                } else if (graphResponse.getError() != null) {
                    switch (graphResponse.getError().getCategory()) {
                        case LOGIN_RECOVERABLE:
                            Log.d("Error",
                                    "Authentication error: " + graphResponse.getError());
                            break;

                        case TRANSIENT:
                            Log.d("Error",
                                    "Transient error. Try again. " + graphResponse.getError());
                            break;

                        case OTHER:
                            Log.d("Error",
                                    "Some other error: " + graphResponse.getError());
                            break;
                    }
                }
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "email,name");
        request.setParameters(parameters);
        request.executeAsync();
    }


    private void fbLoginDone(String email, String name) {
        //  Log.d(ShopperCrux.TAG, "FB Login done");
//        if (progressDialog.isShowing())
//            progressDialog.dismiss();
//
//        SharedPreferences.Editor editor = ShopperCrux.mUserPreferences.edit();
//        editor.putString(ParseConstants.USER_USER_NAME, email);
//        editor.putString(ParseConstants.USER_PASSWORD, password);
//        editor.putString(ParseConstants.USER_NAME, name);
//        editor.commit();
//
//        LoginManager.getInstance().logOut();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

        /*if (ParseUser.getCurrentUser() != null) {
            // Start an intent for the logged in activity
            ParseUser currentUser = ParseUser.getCurrentUser();
            if ((currentUser != null) && currentUser.isAuthenticated()) {
                storeFacebookData();
            }
            //startActivity(new Intent(this, MainActivity.class));
            //finish();
        }*/
    }


    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void setupTextDetails() {
        // mTextDetails = (TextView) view.findViewById(R.id.text_details);
    }

    private void setupTokenTracker() {
        mTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                Log.d("VIVZ", "" + currentAccessToken);
            }
        };
    }


    private void setupProfileTracker() {
        mProfileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                Log.d("VIVZ", "" + currentProfile);
                //mTextDetails.setText(constructWelcomeMessage(currentProfile));
            }
        };
    }

    private void setupLoginButton() {
        LoginButton mButtonLogin = (LoginButton) findViewById(R.id.login_button);
        mButtonLogin.setReadPermissions("user_friends");
        mButtonLogin.registerCallback(mCallbackManager, mFacebookCallback);
    }

    private String constructWelcomeMessage(Profile profile) {
        StringBuffer stringBuffer = new StringBuffer();
        if (profile != null) {
            stringBuffer.append("Welcome " + profile.getName());
        }
        return stringBuffer.toString();
    }

    private void handleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            session.setLogin(true);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("UserName", acct.getDisplayName());
            intent.putExtra("Email", acct.getEmail());
            startActivity(intent);
        } else {
            // Signed out, show unauthenticated UI.
            // updateUI(false);
        }
    }

    /**
     * function to verify login details in mysql db
     */
    private void checkLogin(final String email, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_login";

        pDialog.setMessage("Logging in ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_LOGIN, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d("ShopperCrux", "Login Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // user successfully logged in
                        // Create login session
                        session.setLogin(true);

                        JSONObject user = jObj.getJSONObject("user");
                        String name = user.getString("firstname");
                        String email = user.getString("email");
                        String phone = user.getString("phone");
                        String password = user.getString("password");
                        String cid = user.getString("id");

                        // Inserting row in users table
                        db.addUser(cid, name, email, phone, password);

                        // Launch main activity
                        Intent intent = new Intent(LoginActivity.this,
                                MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("ShopperCrux", "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("password", password);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register connection status listener
        AppController.getInstance().setConnectivityListener(this);
        Profile profile = Profile.getCurrentProfile();
        // mTextDetails.setText(constructWelcomeMessage(profile));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTokenTracker.stopTracking();
        mProfileTracker.stopTracking();
    }


    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        showSnack(isConnected);
    }

    // Method to manually check connection status
    private void checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        showSnack(isConnected);
    }

    // Showing the status in Snackbar
    private void showSnack(boolean isConnected) {
        String message;
        Snackbar snackbar;
        int color;
        if (isConnected) {
            message = "Connected to Internet";
            color = Color.WHITE;
            snackbar = Snackbar
                    .make(scrollView, message, Snackbar.LENGTH_LONG);
        } else {
            message = "Not connected to internet !!";
            color = Color.RED;
            snackbar = Snackbar
                    .make(scrollView, message, Snackbar.LENGTH_INDEFINITE);
        }

        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(color);
        snackbar.show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
