PCAPdroid exposes an API for other apps to configure and control the packet capture. This can be used to easily enable packet capture in your app, provided that the PCAPdroid app is also installed into the device.

## The API

The [CaptureCtrl.java](https://github.com/emanuele-f/PCAPdroid/blob/master/app/src/main/java/com/emanuelef/remote_capture/activities/CaptureCtrl.java) activity is the one exposed to allow you to control the PCAPdroid capture via Intents.

The activity can be easily invoked from the cli by running:

```bash
adb shell am start -e action [ACTION] -e [SETTINGS] -n com.emanuelef.remote_capture/.activities.CaptureCtrl
```

where ACTION is one of:
  - `start`: starts the capture
  - `stop`: stops the capture

The capture parameters are specified via Intent extras, which are discussed below.
A common task is to capture the traffic of a specific app to analyze it into your app. This can be easily accomplished by running PCAPdroid in the
[UDP Exporter mode](https://emanuele-f.github.io/PCAPdroid/dump_modes#24-udp-exporter):

```bash
adb shell am start -e action start -e pcap_dump_mode udp_exporter -e collector_ip_address 127.0.0.1 -e collector_port 5123 -e app_filter org.mozilla.firefox -n com.emanuelef.remote_capture/.activities.CaptureCtrl
```

then your app can listen for UDP packets on port `5123` to handle the Firefox network packets.
Another interesting option is to enable the [pcapdroid_trailer](https://emanuele-f.github.io/PCAPdroid/advanced_features#45-pcapdroid-trailer) to be able to get the app UID/name into your app.

The Intent above can also be triggered programmatically from your app:

```java
class YourActivity extends Activity {
  private final ActivityResultLauncher<Intent> captureLauncher =
    registerForActivityResult(new StartActivityForResult(), this::handleCaptureResult);

  void startCapture() {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setClassName("com.emanuelef.remote_capture", "com.emanuelef.remote_capture.activities.CaptureCtrl");

    intent.putExtra("action", "start");
    intent.putExtra("pcap_dump_mode", "udp_exporter");
    intent.putExtra("collector_ip_address", "127.0.0.1");
    intent.putExtra("collector_port", "5123");
    intent.putExtra("app_filter", "org.mozilla.firefox");

    captureLauncher.launch(intent);
  }

  void handleCaptureResult(final ActivityResult result) {
    if(result.getResultCode() == RESULT_OK) {
      // command executed successfully
    }
  }
}
```

The result code tells if the command succeded or not. Check out the [PCAPReceiver sample app](https://github.com/emanuele-f/PCAPReceiver) for a working example.

## User Consent

To prevent malicious apps from monitoring/hijacking the device traffic, PCAPdroid will ask for user consent every time a capture is started. If the user denies consent, then the request fails.
Consent is also asked when a stop request is received from a different app from the one which started it.

Please note that AFAIK Android does not provide a consistent way to determine the source of an Intent.
This is only available via [getCallingPackage](https://developer.android.com/reference/android/app/Activity#getCallingPackage()) when the caller app uses `startActivityForResult` (or the equivalent `ActivityResultLauncher`). So it's adviced to always invoke the API via `startActivityForResult`.

## Capture Settings

As shown above, the capture settings can be specified by using intent extras. The updated list of all the supported parameters is available in
[CaptureSettings.java](https://github.com/emanuele-f/PCAPdroid/blob/master/app/src/main/java/com/emanuelef/remote_capture/model/CaptureSettings.java).

| Parameter               | Type   | Value                                                             |
|-------------------------|--------|-------------------------------------------------------------------|
| pcap_dump_mode          | string | none \| http_server \| udp_exporter \| pcap_file                  |
| app_filter              | string | the package name of the app to capture                            |
| collector_ip_address    | string | the IP address of the collector in udp_exporter mode              |
| collector_port          | int    | the UDP port of the collector in udp_exporter mode                |
| http_server_port        | int    | the HTTP server port in http_server mode                          |
| pcap_uri                | string | the URI for the PCAP dump in pcap_file mode                       |
| tls_decryption_enabled  | bool   | true to enable the SOCKS5 proxy (e.g. for TLS decryption)         |
| socks5_proxy_ip_address | string | the IP address of the SOCKS5 proxy                                |
| socks5_proxy_port       | int    | the TCP port of the SOCKS5 proxy                                  |
| ipv6_enabled            | bool   | true to enable IPv6 support in non-root mode                      |
| root_capture            | bool   | true to capture packets in root mode, false to use the VPNService |
| pcapdroid_trailer       | bool   | true to enable the PCAPdroid trailer                              |
| capture_interface       | string | @inet \| any \| ifname - network interface to use in root mode    |

*NOTE*: due to [file storage restrictions](https://developer.android.com/about/versions/11/privacy/storage), the `pcap_uri` must point to an app internal directory, e.g. `file:///data/user/0/com.emanuelef.remote_capture/cache/dump.pcap`.

## Dumping PCAP to file

Due to the restrictions introduced via the [scoped storage](https://developer.android.com/about/versions/11/privacy/storage), PCAPdroid can only create files inside its private directory, which is not accessible to you as a user. To dump the PCAP file to a publicly available directory, you must first perform the following steps:

1. Open PCAPdroid and select the "PCAP File" dump mode
2. Start the capture and select the path of the file to write. In this example I assume you select the `/sdcard/test.pcap` file
3. Stop the capture and choose to keep the generated PCAP file (don't delete it!)
4. Retrieve the internal URL which Android uses to reference this file. You can find this in the logcat output of PCAPdroid:

```
D/Main: PCAP URI to write: content://com.android.externalstorage.documents/document/primary%3Atest.pcap
```

You should now be able to write the `test.pcap` file by setting the `pcap_uri` to this URI. You must repeat the steps above if you delete the file.
