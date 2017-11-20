package com.example.nfctest;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String lang = "en";
    private static final byte[] langBytes = lang.getBytes();
    private static final int langLength = langBytes.length;

    private static final String URL = "http://questin.co";

    private IntentFilter[] intentFiltersArray;
    private String[][] techListArray;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private TextView textNfcData;
    private EditText editData;
    private Button btnWrite;
    private Button btnRead;
    private TextView textRead;

    private Tag nfcTag;
    private Parcelable[] rawMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textNfcData = (TextView) findViewById(R.id.text_nfc_data);
        editData = (EditText) findViewById(R.id.edit_data);
        btnWrite = (Button) findViewById(R.id.btn_write);
        btnRead = (Button) findViewById(R.id.btn_read);
        textRead = (TextView) findViewById(R.id.text_read_data);
        setForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListArray);
    }

    private void setForegroundDispatch(){
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            intentFilter.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        intentFiltersArray = new IntentFilter[]{intentFilter,};
        techListArray = new String[][]{new String[]{Ndef.class.getName()},
                new String[]{MifareUltralight.class.getName()}};
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private NdefRecord createTextRecord(String text, boolean encodeInUtf8){
        byte[] langBytes = "en".getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1<<7);
        byte[] payload = new byte[1 + langBytes.length + textBytes.length];
        char status = (char) (utfBit + langBytes.length);
        payload[0] = (byte) status;
        System.arraycopy(langBytes, 0, payload, 1, langBytes.length);
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.length, textBytes.length);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    private NdefRecord createFilterRecord(){
//        return NdefRecord.createExternal("co.questin", "externalType", new byte[0]);
        return NdefRecord.createUri(URL);
    }

    private NdefRecord createAarRecord(){
        return NdefRecord.createApplicationRecord("com.buggyarts.birdsmash");
    }

    private NdefMessage getNdefMessage(){
        return new NdefMessage(
                new NdefRecord[]{
                        createTextRecord(editData.getText().toString(), true), createAarRecord()}
        );
    }

    private void writeToTag(Tag tag){
        Ndef ndef = Ndef.get(tag);
        if(ndef != null){
            try {
                ndef.connect();
                Toast.makeText(getApplicationContext(), "Connected to Card", Toast.LENGTH_SHORT).show();
                NdefMessage ndefMsg = getNdefMessage();
                ndef.writeNdefMessage(ndefMsg);
                Toast.makeText(getApplicationContext(), "Written to Card", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "writeToTag written ndef: " + ndefMsg.toString());
            } catch (IOException | FormatException e) {
                e.printStackTrace();
            } finally {
                try {
                    ndef.close();
                    Toast.makeText(getApplicationContext(), "Disconnected from Card", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if(ndefFormatable != null){
                try {
                    ndefFormatable.connect();
                    Toast.makeText(getApplicationContext(), "Connected to Card", Toast.LENGTH_SHORT).show();
                    NdefMessage ndefMsg = getNdefMessage();
                    ndefFormatable.format(ndefMsg);
                    Toast.makeText(getApplicationContext(), "Written to Card", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "writeToTag written ndefFormatable: " + ndefMsg.toString());
                } catch (IOException | FormatException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        ndefFormatable.close();
                        Toast.makeText(getApplicationContext(), "Disconnected from Card", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void readFromTag(Parcelable[] rawMessages){
        if(rawMessages != null){
            NdefMessage[] messages = new NdefMessage[rawMessages.length];
            for(int i=0; i<messages.length; i++){
                messages[i] = (NdefMessage) rawMessages[i];
                Log.e(TAG, "onCreate: " + messages[i].toString());
                Log.e(TAG, "onCreate: " + new String(messages[i].getRecords()[0].getPayload()));
                textRead.setText(new String(messages[i].getRecords()[0].getPayload()));
            }
        }
        else {
            Log.e(TAG, "onCreate: rawMessages is null");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "onNewIntent: " + intent.getAction());
        final Tag nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        Log.e(TAG, "onCreate: " + intent.getAction());
        Log.e(TAG, "onCreate: " + Arrays.toString(nfcTag.getTechList()));
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeToTag(nfcTag);
            }
        });

        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readFromTag(rawMessages);
            }
        });
    }

    private String readTagMifare(Tag tag){
        MifareUltralight mifare = MifareUltralight.get(tag);
        try {
            mifare.connect();
            byte[] payload = mifare.readPages(4);
            return new String(payload, Charset.forName("US-ASCII"));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "readTag: "+ e.getMessage());
        } finally {
            if(mifare != null){
                try {
                    mifare.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String readTagIsoDep(Tag tag){
        IsoDep specificTag = IsoDep.get(tag);
        try {
            specificTag.connect();
            byte[] payload = specificTag.getHiLayerResponse();
            return new String(payload, Charset.forName("US-ASCII"));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "readTag: "+ e.getMessage());
        } finally {
            if(specificTag != null){
                try {
                    specificTag.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String readTagNfcA(Tag tag){
        NfcA specificTag = NfcA.get(tag);
        try {
            specificTag.connect();
            byte[] payload = specificTag.getAtqa();
            return String.format("%02x", payload);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "readTag: "+ e.getMessage());
        } finally {
            if(specificTag != null){
                try {
                    specificTag.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}
