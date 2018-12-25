package sanji.com.service;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * 无界面Activity或者APP的实现
 * https://blog.csdn.net/cqx13763055264/article/details/80037162
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.finish();
    }
}
