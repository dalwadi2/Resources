package raghav.resources.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.support.base.CoreActivity;

import raghav.resources.R;

public class MainActivity extends CoreActivity {

    private MainActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setDefaults(activity, R.layout.activity_main, "", false, true);
    }

    public void openListActivity(View view) {
        startActivity(new Intent(this, ListActivity.class));
    }

    public void openSingleActivity(View view) {
        startActivity(new Intent(this, SingleActivity.class));
    }

    public void openImageSelectorActivity(View view) {
        startActivity(new Intent(this, ImageSelectorActivity.class));
    }

    @Override
    public void createReference() {

    }

    @Override
    protected void setListeners(boolean state) {

    }
}
