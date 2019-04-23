package com.philips.ssa.ssa_android_app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.philips.ai.SmartSleepAnalyzerClient;
import com.philips.ai.models.SenseDTO;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CalculateBMIActivity extends AppCompatActivity {

    private SmartSleepAnalyzerClient client = null;
    private List<SenseDTO> questionSenses = null;
    private ComputeSenseTask mComputeTask = null;
    private InitializeClientTask mInitializeClientTask = null;

    private LinearLayout parentLinearLayout = null;
    private View mProgressView;
    private View mSenseFormView;

    private String sense;
    private String output;
    private String clientId;
    private String clientSecret;
    private String lastActivity_accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_bmi);

        parentLinearLayout = findViewById(R.id.bmi_dynamic);
        mSenseFormView = findViewById(R.id.bmi_form);
        mProgressView = findViewById(R.id.sense_progress);

        Intent getAccessToken = getIntent();
        Bundle extras = getAccessToken.getExtras();
        clientId = extras == null ? "" : extras.getString("clientId");
        clientSecret = extras == null ? "" : extras.getString("clientSecret");

        Button btn_CalculateSense = findViewById(R.id.btn_CalculateSense);

        btn_CalculateSense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalculateSense_Click();
            }
        });

        sense = "bmi";
        BuildFormControls();
    }

    private void BuildFormControls() {
        try {

            if (mInitializeClientTask != null) {
                return;
            }

            mInitializeClientTask = new InitializeClientTask(clientId, clientSecret);
            mInitializeClientTask.execute((Void) null).get();

            LinearLayout dynamicLayout = findViewById(R.id.bmi_dynamic);

            for (SenseDTO question : questionSenses) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (question.getType().equals("DoubleSenseValue")) {
                    final View rowView = inflater.inflate(R.layout.activity_number_fields, dynamicLayout, false);
                    TextInputLayout layout_number = rowView.findViewById(R.id.txt_numberLayout);
                    layout_number.setHint(question.getQuestionText());
                    parentLinearLayout.addView(rowView, parentLinearLayout.getChildCount());
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void CalculateSense_Click() {

        if (mComputeTask != null) {
            return;
        }

        HashMap<String, Object> inputValues = new HashMap<>();
        boolean cancel = false;
        View focusView = null;

        int childCount = parentLinearLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View thisChild = parentLinearLayout.getChildAt(i);
            EditText txt_number = thisChild.findViewById(R.id.number_field);
            if (txt_number != null) {
                String childTextViewValue = txt_number.getText().toString();
                if (TextUtils.isEmpty(childTextViewValue)) {
                    txt_number.setError(getString(R.string.error_field_required));
                    focusView = txt_number;
                    cancel = true;
                    break;
                } else {
                    inputValues.put(questionSenses.get(i).getId(), childTextViewValue);
                }
            }
        }

        if (cancel) {
            // There was an error; don't attempt execute and focus the first form field with an error.
            focusView.requestFocus();

        } else {
            // Show a progress spinner, and kick off a background task to compute the sense
            showProgress(true);

            mComputeTask = new ComputeSenseTask(sense, inputValues);

            try {
                mComputeTask.execute((Void) null).get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!TextUtils.isEmpty(output))
                openDialog(output);
            else
                SetNextActivity();
        }
    }

    private void SetNextActivity() {
        Intent nextActivityBmiView = new Intent(this, CalculateBMIActivity.class);

        nextActivityBmiView.putExtra(getString(R.string.clientId), clientId);
        nextActivityBmiView.putExtra(getString(R.string.clientSecret), clientSecret);
        startActivity(nextActivityBmiView);
        finish();
    }

    private void openDialog(String message) {

        if (message.contains(getString(R.string.err_token_expired))) {
            message = getString(R.string.token_expired);
        } else if (!TextUtils.isEmpty(message)) {
            try {
                DecimalFormat df = new DecimalFormat("0.0");
                df.setMaximumFractionDigits(2);
                message = "Your BMI is: " + df.format(Float.parseFloat(output));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            message = getString(R.string.generic_error);
        }

        AlertDialog builder = new AlertDialog.Builder(CalculateBMIActivity.this).create();
        builder.setTitle("INFORMATION!");
        builder.setMessage(message);
        builder.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int childCount = parentLinearLayout.getChildCount();
                        for (int i = 0; i < childCount; i++) {
                            View thisChild = parentLinearLayout.getChildAt(i);
                            EditText txt_number = thisChild.findViewById(R.id.number_field);
                            if (txt_number != null) {
                                txt_number.setText("");
                            }
                        }
                        output = "";
                        dialog.dismiss();
                    }
                });
        builder.show();
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

            mSenseFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mSenseFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSenseFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mSenseFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous compute sense task used to get computed sense.
     */
    class ComputeSenseTask extends AsyncTask<Void, Void, Boolean> {

        private final String mSenseId;
        private final HashMap<String, Object> mInputValues;

        ComputeSenseTask(String senseId, HashMap<String, Object> inputValues) {
            mSenseId = senseId;
            mInputValues = inputValues;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                LinkedHashMap<String, Object> computeSenseMap = client.getScoring().computeSense(mSenseId, mInputValues);

                Object id = computeSenseMap.get(mSenseId);
                output = id == null ? "" : id.toString();
                if (!TextUtils.isEmpty(output)) {
                    return true;
                }

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mComputeTask = null;
            showProgress(false);
        }

        @Override
        protected void onCancelled() {
            mComputeTask = null;
            showProgress(false);
        }
    }

    class InitializeClientTask extends AsyncTask<Void, Void, Boolean> {

        private final String mClientId;
        private final String mClientSecret;

        InitializeClientTask(String clientId, String clientSecret) {
            mClientId = clientSecret;
            mClientSecret = clientSecret;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                client = new SmartSleepAnalyzerClient(clientId, clientSecret);
                questionSenses = client.getScoring().getRequiredQuestionSenses(sense, "en-US");
                return true;
            } catch (
                    Throwable throwable) {
                throwable.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mComputeTask = null;
            showProgress(false);
        }

        @Override
        protected void onCancelled() {
            mComputeTask = null;
            showProgress(false);
        }
    }
}