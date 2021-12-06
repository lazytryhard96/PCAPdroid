package com.emanuelef.remote_capture;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

    //Roboletric context
    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void formatBytesTest() {

        // ...when the string is returned from the object under test...
        String result = Utils.formatBytes(1L);

        assertThat(result).isEqualTo("1 B");

        result = Utils.formatBytes((1024 * 1024 - 1));

        assertThat(result).isEqualTo("1024.0 KB");

        result = Utils.formatBytes((1024 * 1024 + 1));

        assertThat(result).isEqualTo("1.0 MB");

        result = Utils.formatBytes((1024 * 1024 * 1024 + 1));

        assertThat(result).isEqualTo( "1.0 GB");
    }

    @Test
    public void formatPktsTest(){

        String result = Utils.formatPkts(1L);

        assertThat(result).isEqualTo("1");

        result = Utils.formatPkts((1000 * 1000 - 1));

        assertThat(result).isEqualTo("1000.0 K");

        result = Utils.formatPkts((1000 * 1000 + 1));

        assertThat(result).isEqualTo("1.0 M");

        result = Utils.formatPkts((1000 * 1000 * 1000 + 1));

        assertThat(result).isEqualTo( "1.0 G");
    }

    @Test
    public void testIsRTLFalse() {
        setLocale("en");
        Boolean aBoolean = Utils.isRTL(context);
        assertEquals(false, aBoolean);
    }

    @Test
    public void testIsRTLTrue() {
        setLocale("ar");
        Boolean aBoolean = Utils.isRTL(context);
        assertEquals(true, aBoolean);
    }

    @Test
    public void testLocaleZH() {
        setLocale("zh");
        String string = context.getResources().getString(R.string.app);
        assertThat(string).isEqualTo( "应用程序");
    }

    @Test
    public void testLocaleEN() {
        setLocale("en");
        String string = context.getResources().getString(R.string.app);
        assertThat(string).isEqualTo( "App");
    }


    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        // here we update locale for date formatters
        Locale.setDefault(locale);
        // here we update locale for app resources
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }


}

