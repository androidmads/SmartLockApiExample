package com.ajts.androidmads.smartlockapiexample;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    static final String TAG = "MainActivity";
    static final int RC_SAVE = 1;
    static final int RC_READ = 2;
    String username = "1234";
    String password = "1234";
    EditText usernameEditText, passwordEditText;
    Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        signInButton = (Button) findViewById(R.id.signInButton);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username1 = usernameEditText.getText().toString();
                String password1 = passwordEditText.getText().toString();
                if (!(username.equals(username1) && password.equals(password1))) {
                    return;
                }
                Credential credential = new Credential.Builder(username1)
                        .setPassword(password1)
                        .build();
                saveCredential(credential);
            }
        });
    }

    protected void saveCredential(Credential credential) {
        // Credential is valid so save it.
        Auth.CredentialsApi.save(mGoogleApiClient,
                credential).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "Credential saved");
                    goToContent();
                } else {
                    Log.d(TAG, "Attempt to save credential failed " +
                            status.getStatusMessage() + " " +
                            status.getStatusCode());
                    resolveResult(status, RC_SAVE);
                }
            }
        });
    }

    private void resolveResult(Status status, int requestCode) {
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(this, requestCode);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
            }
        } else {
            if (requestCode == RC_SAVE) {
                goToContent();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        if (requestCode == RC_SAVE) {
            Log.d(TAG, "Result code: " + resultCode);
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Credential Save: OK");
            } else {
                Log.e(TAG, "Credential Save Failed");
            }
            goToContent();
        }
        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                processRetrievedCredential(credential);
            } else {
                Log.e(TAG, "Credential Read: NOT OK");
            }
        }
    }

    /**
     * Start the Content Activity and finish this one.
     */
    protected void goToContent() {
        startActivity(new Intent(this, ContentActivity.class));
        finish();
    }

    private void requestCredentials() {
        Log.d(TAG, "requestCredentials");
        CredentialRequest request = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build();

        Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                        Status status = credentialRequestResult.getStatus();
                        Log.v(TAG, status.getStatus().toString());
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            Credential credential = credentialRequestResult.getCredential();
                            processRetrievedCredential(credential);
                        } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                            Log.d(TAG, "Sign in required");
                        } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                            Log.w(TAG, "Unrecognized status code: " + status.getStatusCode());
                            try {
                                status.startResolutionForResult(MainActivity.this, RC_READ);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "STATUS: Failed to send resolution.", e);
                            }
                        }
                    }
                }
        );

    }

    private void processRetrievedCredential(Credential credential) {
        if (username.equals(credential.getId()) && password.equals(credential.getPassword())) {
            goToContent();
        } else {
            Log.d(TAG, "Retrieved credential invalid, so delete retrieved" + " credential.");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestCredentials();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
