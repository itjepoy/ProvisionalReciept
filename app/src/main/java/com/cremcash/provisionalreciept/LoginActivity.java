package com.cremcash.provisionalreciept;

import static com.cremcash.provisionalreciept.ConnectionClass.ip;
import static com.cremcash.provisionalreciept.ConnectionClass.pass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/** @noinspection ALL*/
public class LoginActivity extends AppCompatActivity {

    TextInputEditText usernamelogin,passwordlogin;
    Button loginbtn;
    Connection con;

    private SessionManager session;
    private SQLiteHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        usernamelogin = findViewById(R.id.username);
        passwordlogin = findViewById(R.id.password);
        loginbtn = findViewById(R.id.btn_login);

        loginbtn.setOnClickListener(v -> {
            new login_check().execute();
        });

        db = new SQLiteHandler(getApplicationContext());
        session = new SessionManager(getApplicationContext());

        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        usernamelogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(Objects.requireNonNull(usernamelogin.getText()).toString().isEmpty()){
                    usernamelogin.setError("Enter Username");
                }else {
                    usernamelogin.setError(null);
                }
            }
        });

        passwordlogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(passwordlogin.getText().toString().length() <=0){
                    passwordlogin.setError("Enter Password");
                }else {
                    passwordlogin.setError(null);
                }
            }
        });

    }

    public String hashMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b)); // Convert bytes to hex format
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class login_check extends AsyncTask<String, String, String> {

        String z = null;
        Boolean isSuccess = null;
        ProgressDialog progressDialog;
        String uid = null, username = null, name = null, created_at = null;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Logging in, please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                con = connectionClass("sa", "g@t3k33p3R2024", "CAS", "10.10.0.73");
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "You are not connected to network. Offline mode will activate", Toast.LENGTH_LONG).show());
                    Log.d("Network Error", "Not Connected to Server");
                } else {
                    String hashedPassword = hashMD5(String.valueOf(passwordlogin.getText()));

                    String sql = "SELECT UA.Employee_ID, Emp.Fullname FROM [CAS].[dbo].[UserAccounts] UA " +
                            "LEFT JOIN Employees Emp ON UA.Employee_ID = Emp.EmployeeID " +
                            "WHERE UA.Employee_ID = ? AND UA.Password = ?";

                    try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                        pstmt.setString(1, String.valueOf(usernamelogin.getText()));
                        pstmt.setString(2, hashedPassword);

                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                isSuccess = true;

                                // Extract data from ResultSet
                                uid = rs.getString("Employee_ID");
                                username = uid;
                                name = rs.getString("Fullname");

                                Date c = Calendar.getInstance().getTime();
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("MMddyyyy");
                                created_at = df.format(c);

                            } else {
                                isSuccess = false;
                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Check username or password", Toast.LENGTH_LONG).show();
                                    usernamelogin.setText("");
                                    passwordlogin.setText("");
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                isSuccess = false;
                Log.e("SQL Error : ", Objects.requireNonNull(e.getMessage()));
            }
            return z;
        }

        @Override
        protected void onPostExecute(String s) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (isSuccess != null && isSuccess) {
                Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
                session.setLogin(true);
                session.setUserId(uid);
                db.addUser(name, username, uid, created_at);

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                LoginActivity.this.startActivity(intent);
                ((Activity) LoginActivity.this).finish();
            }
        }
    }



    public void checkLogin () {
        con = connectionClass(ConnectionClass.un, pass, "CAS", ip);
        if(con == null){
            runOnUiThread(() -> {
                Toast.makeText(LoginActivity.this,"You are not connected to network. Offline mode will activate",Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
            Log.d("Network Error","Not Connected to Server");
        } else {
            try {
                String sql = "SELECT UA.Employee_ID, Emp.Fullname FROM [CAS].[dbo].[UserAccounts] UA LEFT JOIN Employees Emp ON UA.Employee_ID = Emp.EmployeeID  WHERE UA.Employee_ID = = '" + usernamelogin.getText() + "' AND UA.Password = '" + passwordlogin.getText() + "' ";
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_SHORT).show());
                    Log.d("Network Connected","Login Success");
                    //session.setLogin(true);


                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("key", rs.getString(Objects.requireNonNull(usernamelogin.getText()).toString()));
                    startActivity(intent);
                    finish();

                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Check employee id or password", Toast.LENGTH_LONG).show());

                    usernamelogin.setText("");
                    passwordlogin.setText("");
                }
            } catch (Exception e) {
                Log.e("SQL Error : ", e.getMessage());
            }
        }
    }

    @SuppressLint("NewApi")
    public Connection connectionClass(String user, String pass, String db, String ip) {
        Connection connection = null;
        String ConnectionURL;

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver"); // Ensure the driver is added in dependencies
            ConnectionURL = "jdbc:jtds:sqlserver://" + ip + ";databaseName=" + db + ";user=" + user + ";password=" + pass + ";";
            connection = DriverManager.getConnection(ConnectionURL);
        } catch (Exception ex) {
            Log.e("DB Connection Error", Objects.requireNonNull(ex.getMessage()));
        }

        return connection;


    }
}