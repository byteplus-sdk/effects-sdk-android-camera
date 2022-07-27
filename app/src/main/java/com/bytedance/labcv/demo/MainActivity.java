package com.bytedance.labcv.demo;

import android.content.Context;
import android.os.Bundle;

import com.bytedance.labcv.demo.task.RequestLicenseTask;
import com.bytedance.labcv.demo.task.UnzipTask;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bytedance.labcv.demo.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements UnzipTask.IUnzipViewCallback {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // unzip task
        UnzipTask task = new UnzipTask(this);
        task.execute(UnzipTask.DIR);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // ---------------------- Implement UnzipTask Callbacks ---------------------------
    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public void onStartTask() {
        System.out.println("UnzipTask started");
    }

    @Override
    public void onEndTask(boolean result) {
        if(!result){
            System.out.println("ERROR: Failed to copy effects resources.");
        }
        checkLicenseReady();
    }


    public void checkLicenseReady() {
        RequestLicenseTask task = new RequestLicenseTask(new RequestLicenseTask.ILicenseViewCallback() {

            @Override
            public Context getContext() {
                return getApplicationContext();
            }

            @Override
            public void onStartTask() {
                System.out.println("license request started");
            }

            @Override
            public void onEndTask(boolean result) {

            }
        });
        task.execute();

    }
}