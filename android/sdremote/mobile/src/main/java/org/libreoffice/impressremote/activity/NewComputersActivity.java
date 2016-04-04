package org.libreoffice.impressremote.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.adapter.ComputersPagerAdapter;
import org.libreoffice.impressremote.fragment.ComputersFragment;
import org.libreoffice.impressremote.util.Intents;

public class NewComputersActivity extends AppCompatActivity {

    private static final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_computers);

        //Toolbar = ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*We need to do this because the name of the activity is "Impress Remote" by default,
         *(because it's a launcher activity), and we need to change it to "Computers" */
        getSupportActionBar().setTitle(R.string.title_computers);

        ComputersPagerAdapter computersPagerAdapter = new ComputersPagerAdapter(getSupportFragmentManager());

        computersPagerAdapter.addFragment(ComputersFragment.Type.WIFI);

        //add bluetooth tab only if bluetooth is available
        if(btAdapter != null) {
            computersPagerAdapter.addFragment(ComputersFragment.Type.BLUETOOTH);
        }


        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(computersPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        //we need this context for the FAB
        final Context context = this;

        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent aIntent = Intents.buildComputerCreationIntent(context);
                startActivity(aIntent);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_action_bar_computers, menu);

        //if bluetooth is not present, remove bt toggle from menu
        if (btAdapter == null){
            menu.removeItem(R.id.menu_start_discovery);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.menu_settings){
            Intent aIntent = Intents.buildSettingsIntent(this);
            startActivity(aIntent);
        }

        if (id == R.id.menu_requirements) {
            Intent intent = Intents.buildRequirementsIntent(this);
            startActivity(intent);
        }
        else if (id == R.id.menu_start_discovery){
            btAdapter.enable();
            btAdapter.startDiscovery();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //disable bluetooth after exiting the application
        btAdapter.disable();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_new_computers, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
}
