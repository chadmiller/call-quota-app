package org.chad.jeejah.callquota;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.util.Log;

import android.provider.Contacts.People;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class FreeContacts extends ListActivity {
    private static final String TAG = "SeeStats.FreeContacts";

    private String[] queriedProjection = { 
            Contacts.PhonesColumns.NUMBER_KEY, 
            Contacts.PhonesColumns.NUMBER, 
            Contacts.PhonesColumns.TYPE,
            Contacts.PhonesColumns.LABEL,
            Contacts.PeopleColumns.DISPLAY_NAME,
        };

    private String[] storedProjection = { 
            Contacts.PhonesColumns.NUMBER_KEY, 
            Contacts.PhonesColumns.NUMBER 
        };

    private String[] keyList = { Contacts.PhonesColumns.NUMBER_KEY };
    private Set<ContactInfo> prettyDisplayBacking = new TreeSet<ContactInfo>();
    private Set<ContactInfo> contacts = new TreeSet<ContactInfo>();

    private class ContactInfo implements Comparable<ContactInfo> {
        private static final String TAG = "SeeStats.FreeContacts.ContactInfo";

        public String name;
        public String number;
        public String numberKey;
        public String label;

        public ContactInfo(String number, String numberKey) {
            this.number = number;
            this.numberKey = numberKey;
        }

        public ContactInfo(String name, String number, String numberKey, String label) {
            this.name = name;
            this.number = number;
            this.numberKey = numberKey;
            this.label = label;
        }

        public int compareTo(ContactInfo other) {
            Log.d(TAG, "compareTo()");
            return this.numberKey.compareTo(other.numberKey);
        }

        public boolean equals(ContactInfo other) {
            Log.d(TAG, "equals()");
            return this.numberKey.equals(other.numberKey);
        }
    }

    private Set<ContactInfo> getFreeContacts() {
        return contacts;
    }

    private MatrixCursor getFreeContactsCursor() {
        MatrixCursor mc = new MatrixCursor(storedProjection);
        for (ContactInfo c: getFreeContacts()) {
            String row[] = {c.number, c.numberKey};
            mc.addRow(row);
        }

        return mc;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle(R.string.title_free_contacts);
        setContentView(R.layout.freecontacts);

        TextView adderItem = new TextView(this);
        adderItem.setText("Add new contact");
        adderItem.setPadding(8, 16, 8, 16);
        adderItem.setTextSize(20);
        getListView().addFooterView(adderItem);

    }

    @Override
    public void onResume() {
        super.onResume();

        MatrixCursor freeListCursor = getFreeContactsCursor();

        ContentResolver cr = getContentResolver();
        Cursor allContactsCursor = cr.query(Contacts.Phones.CONTENT_URI, queriedProjection, null, null, Contacts.PhonesColumns.NUMBER_KEY);

        final int accNamePosition = allContactsCursor.getColumnIndexOrThrow(Contacts.PeopleColumns.DISPLAY_NAME);
        final int accLabelPosition = allContactsCursor.getColumnIndexOrThrow(Contacts.PhonesColumns.LABEL);
        final int accLabelTypePosition = allContactsCursor.getColumnIndexOrThrow(Contacts.PhonesColumns.TYPE);
        final int accNumberPosition = allContactsCursor.getColumnIndexOrThrow(Contacts.PhonesColumns.NUMBER);
        final int accNumberKeyPosition = allContactsCursor.getColumnIndexOrThrow(Contacts.PhonesColumns.NUMBER_KEY);
        final int flcNumberPosition = freeListCursor.getColumnIndexOrThrow(Contacts.PhonesColumns.NUMBER);
        final int flcNumberKeyPosition = freeListCursor.getColumnIndexOrThrow(Contacts.PhonesColumns.NUMBER_KEY);

        prettyDisplayBacking.clear();
        CursorJoiner joiner = new CursorJoiner(freeListCursor, keyList, allContactsCursor, keyList);
        for (CursorJoiner.Result joinerResult: joiner) {
            switch (joinerResult) {
                case LEFT:
                    prettyDisplayBacking.add(new ContactInfo("?", freeListCursor.getString(flcNumberPosition), freeListCursor.getString(flcNumberKeyPosition), "(orphaned)"));
                    Log.d(TAG, freeListCursor.getString(flcNumberPosition) + " == n");
                    Log.d(TAG, freeListCursor.getString(flcNumberKeyPosition) + " == nk");
                    break;
                case BOTH:
                    prettyDisplayBacking.add(new ContactInfo(allContactsCursor.getString(accNamePosition), allContactsCursor.getString(accNumberPosition), allContactsCursor.getString(accNumberKeyPosition), getNumberLabel(allContactsCursor.getInt(accLabelTypePosition), allContactsCursor.getString(accLabelPosition))));
                    break;
                // discard all RIGHTs -- those are all contacts' numbers.
            }
        }

        setListAdapter(new SimpleAdapter(this, getData(), R.layout.contacts_list_item, new String[] { "name", "number", "label" }, new int[] { R.id.name, R.id.number, R.id.label }));

    }

    private static String getNumberLabel(int type, String otherText) {
        switch (type) {
            case Contacts.PhonesColumns.TYPE_CUSTOM: return otherText;
            case Contacts.PhonesColumns.TYPE_FAX_HOME: return "Fax Home";
            case Contacts.PhonesColumns.TYPE_FAX_WORK: return "Fax Work";
            case Contacts.PhonesColumns.TYPE_HOME: return "Home";
            case Contacts.PhonesColumns.TYPE_MOBILE: return "Mobile";
            case Contacts.PhonesColumns.TYPE_OTHER: return "Other";
            case Contacts.PhonesColumns.TYPE_PAGER: return "Pager";
            case Contacts.PhonesColumns.TYPE_WORK: return "Work";
            default: return "Other";
        }
    }

    private List getData() {
        List<Map> data = new ArrayList<Map>();
        for (ContactInfo ci: this.prettyDisplayBacking) {
            Map<String, String> temp = new HashMap<String, String>();
            temp.put("name", ci.name);
            temp.put("number", ci.number);
            temp.put("label", ci.label);
            data.add(temp);
        }
        return data;
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (id == -1) {
            Uri peopleUri = Contacts.People.CONTENT_URI;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT, peopleUri);
            i.setType(Contacts.Phones.CONTENT_ITEM_TYPE);
            startActivityForResult(i, 0);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri picked = data.getData();
            ContentResolver cr = getContentResolver();
            Cursor c = cr.query(picked, queriedProjection, null, null, null);

            if (c.moveToFirst()) {
                int numberKeyColumn = c.getColumnIndex(Contacts.PhonesColumns.NUMBER_KEY); 
                int numberColumn = c.getColumnIndex(Contacts.PhonesColumns.NUMBER); 

                do {
                    contacts.add(new ContactInfo(c.getString(numberKeyColumn), c.getString(numberColumn)));
                } while (c.moveToNext());
            } else {
                Log.d(TAG, "Can't reach first row.");
            }
            
        } else {
            Log.d(TAG, "onActivityResult()  unknown resultCode=" + resultCode);
        }
     }

}
/* vim: set et ai sta : */
