package siesgst.edu.in.tml16;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;

import siesgst.edu.in.tml16.fragments.LakshyaEventsFragment;
import siesgst.edu.in.tml16.fragments.NewsFragment;
import siesgst.edu.in.tml16.fragments.RegistrationFragment;
import siesgst.edu.in.tml16.fragments.TatvaEventsFragment;
import siesgst.edu.in.tml16.utils.ConnectionUtils;
import siesgst.edu.in.tml16.utils.DataHandler;
import siesgst.edu.in.tml16.utils.LocalDBHandler;
import siesgst.edu.in.tml16.utils.OnlineDBDownloader;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private ImageView mProfilepic;
    private TextView mUsername;
    private TextView mEmail;

    private Menu menu;
    private ProgressDialog progressDialog;

    private GoogleApiClient mGoogleApiClient;

    final private String dbVersion = "DB_VERSION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set up Goolge Sign in APIs
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        sharedPreferences = getSharedPreferences("TML", MODE_PRIVATE);

        //0: Not logged in
        //1: Skipped
        //2: Logged in

        //Check login status and switch activties accordingly to either Login screen or Home Screen

        //Login Screen
        if (sharedPreferences.getInt("login_status", 0) == 0) {

            startActivityForResult(new Intent(this, LoginActivity.class), 0);
        }

        //Home Screen
        else if (sharedPreferences.getInt("login_status", 0) == 2 | sharedPreferences.getInt("login_status", 0) == 1) {

            setContentView(R.layout.activity_home);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            menu = navigationView.getMenu();
            navigationView.setNavigationItemSelectedListener(this);

            View header = navigationView.getHeaderView(0);

            //Set G+ Profile pic
            mProfilepic = (ImageView) header.findViewById(R.id.profile_pic);
            if (!sharedPreferences.getString("profile_pic", "").equals("")) {
                Picasso.with(this).load(sharedPreferences.getString("profile_pic", "")).into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        mProfilepic.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
            } else {
                mProfilepic.setImageResource(R.mipmap.ic_launcher);
            }

            //Set G+ Username
            mUsername = (TextView) header.findViewById(R.id.username);
            if (sharedPreferences.getInt("login_status", 0) == 1) {
                mUsername.setText("Hi, Guest!");
                hideOption(R.id.sign_out);
                hideOption(R.id.profile);
                showOption(R.id.sign_in);
            } else {
                mUsername.setText(sharedPreferences.getString("username", ""));
                hideOption(R.id.sign_in);
                showOption(R.id.sign_out);
            }

            //Set G+ emailId
            mEmail = (TextView) header.findViewById(R.id.email);
            if (sharedPreferences.getInt("login_status", 0) == 1) {
                mEmail.setVisibility(View.GONE);
            } else {
                mEmail.setText(sharedPreferences.getString("email", ""));
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkDatabaseIntegrity();
                }
            }, 2000);

        }
    }

    public void checkDatabaseIntegrity() {

        int currentDBVersion = sharedPreferences.getInt(dbVersion, 0);
        if ((new LocalDBHandler(this)).getDBVersion() > currentDBVersion) {
            progressDialog = ProgressDialog.show(this, "Syncing Data", "Please wait....");
            if ((new ConnectionUtils(this).checkConnection())) {
                new LocalDBHandler(this).wapasTableBana();
                new EventListDownload().execute();
            } else {
                progressDialog.dismiss();
                showError();
            }
        }
    }

    public void showError() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Please check your internet connection and try again.");
        alertDialog.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkDatabaseIntegrity();
            }
        });
        alertDialog.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    //Handle Login Activity Callback and execute the results obtained.
    // That is, User's G+ username, emailid and profile pic
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {

        editor = sharedPreferences.edit();

        if (requestCode == 0 && responseCode == RESULT_OK) {

            editor.remove("login_status");
            editor.putInt("login_status", 2);
            editor.putString("username", intent.getStringExtra("username"));
            editor.putString("email", intent.getStringExtra("email"));
            editor.putString("profile_pic", intent.getStringExtra("profile_pic"));
            editor.apply();

            //Go back to Home screen once logged in.
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        } else if (requestCode == 0 && responseCode == RESULT_CANCELED) {
            editor.remove("login_status");
            editor.putInt("login_status", 1);
            editor.apply();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        } else if (requestCode == 0 && responseCode == 1) {
            if (sharedPreferences.getBoolean("first_time", true)) {
                if (sharedPreferences.getInt("login_status", 0) == 0) {
                    finish();
                }
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.sign_in :
                startActivityForResult(new Intent(HomeActivity.this, LoginActivity.class), 0);
                break;
            case R.id.sign_out :
                signOut();
                break;
            case R.id.profile :
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                break;
            case R.id.tatva :
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.root_frame, new TatvaEventsFragment())
                        .commit();
                break;
            case R.id.moksh :
                break;
            case R.id.lakshya :
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.root_frame, new LakshyaEventsFragment())
                        .commit();
                break;

            case R.id.news :
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.root_frame, new NewsFragment())
                        .commit();
                break;

            case R.id.venue :
                break;

            case R.id.register :
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.root_frame, new RegistrationFragment())
                        .commit();
                break;

            }

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void hideOption(int id) {
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void showOption(int id) {
        MenuItem item = menu.findItem(id);
        item.setVisible(true);
    }

    private void signOut() {
        editor = sharedPreferences.edit();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Toast.makeText(HomeActivity.this, "Signed out...", Toast.LENGTH_SHORT).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    startActivityForResult(new Intent(HomeActivity.this, LoginActivity.class), 0);
                                }
                            }.start();
                            editor.remove("login_status");
                            editor.remove("username");
                            editor.remove("email");
                            editor.remove("profile_pic");
                            editor.putInt("login_status", 1);
                            editor.apply();
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(HomeActivity.this, "Error Signing in.\nPlease check your internet connection or try again.", Toast.LENGTH_SHORT).show();
    }

    private class EventListDownload extends AsyncTask<Void, Void, JSONArray> {
        JSONArray array;

        @Override
        protected JSONArray doInBackground(Void... params) {
            OnlineDBDownloader downloader = new OnlineDBDownloader(HomeActivity.this);
            downloader.downloadData();
            array = downloader.getJSON();
            return array;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            new DataHandler(HomeActivity.this).decodeAndPushJSON(jsonArray);
            SharedPreferences.Editor editor=sharedPreferences.edit();
            editor.putInt(dbVersion, new LocalDBHandler(getApplicationContext()).getDBVersion());
            editor.apply();
            progressDialog.dismiss();

        }
    }
}