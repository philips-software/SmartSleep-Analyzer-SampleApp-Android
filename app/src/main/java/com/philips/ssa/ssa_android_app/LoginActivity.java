package com.philips.ssa.ssa_android_app;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.philips.ai.SmartSleepAnalyzerClient;
import java.util.concurrent.ExecutionException;

/**
 * A login screen that offers login via client id/client secret.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private String accessToken = null;

    // UI references.
    private AutoCompleteTextView mClientIdView;
    private EditText mClientSecretView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView mErrorMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mClientIdView = findViewById(R.id.clientId);

        mClientSecretView = findViewById(R.id.clientSecret);
        mClientSecretView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mErrorMessageView = findViewById(R.id.txt_error_message) ;
        mErrorMessageView.setVisibility(View.GONE);
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid user name, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        mErrorMessageView.setVisibility(View.GONE);
        // Reset errors.

        mClientIdView.setError(null);
        mClientSecretView.setError(null);

        // Store values at the time of the login attempt.
        String clientId = mClientIdView.getText().toString();
        String clientSecret = mClientSecretView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid client secret, if the user entered one.
        if(TextUtils.isEmpty(clientSecret) )
        {
            mClientSecretView.setError(getString(R.string.error_field_required));
            focusView = mClientSecretView;
            cancel = true;
        }
        else if (!isClientSecretValid(clientSecret)) {
            mClientSecretView.setError(getString(R.string.error_invalid_client_secret));
            focusView = mClientSecretView;

            cancel = true;
        }

        // Check for a empty user name.

        if (TextUtils.isEmpty(clientId)) {
            mClientIdView.setError(getString(R.string.error_field_required));
            focusView = mClientIdView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(clientId, clientSecret);

            try {
                mAuthTask.execute((Void) null).get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!TextUtils.isEmpty(accessToken))
            {
                mErrorMessageView.setText("");
                Intent nextActivityBmiView = new Intent(this, CalculateBMIActivity.class);

                nextActivityBmiView.putExtra(getString(R.string.clientId), clientId);
                nextActivityBmiView.putExtra(getString(R.string.clientSecret), clientSecret);
                startActivity(nextActivityBmiView);
                finish();
            }
            else
            {
                mErrorMessageView.setVisibility(View.VISIBLE);
                mErrorMessageView.setText(getString(R.string.error_invalid_login));
            }
        }
    }

    private boolean isClientSecretValid(String clientSecret) {
        return clientSecret.length() > 4;

    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mClientId;
        private final String mClientSecret;

        UserLoginTask(String clientId, String clientSecret) {
            mClientId = clientId;
            mClientSecret = clientSecret;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Simulate network access.
            SmartSleepAnalyzerClient client  = new SmartSleepAnalyzerClient(mClientId, mClientSecret);
            accessToken = client.geAccessToken();
            return !TextUtils.isEmpty(accessToken);

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mClientSecretView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

