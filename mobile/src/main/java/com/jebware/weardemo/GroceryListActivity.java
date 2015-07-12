package com.jebware.weardemo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class GroceryListActivity extends AppCompatActivity {

    private static final int NOTIFICATION_ID = 10101;

    //TODO comments

    @InjectView(android.R.id.text1)
    EditText inputField;
    @InjectView(android.R.id.list)
    RecyclerView recyclerView;

    private GoogleApiClient googleApiClient;

    private ArrayList<Grocery> groceries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_list);
        ButterKnife.inject(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .build();
        googleApiClient.connect();
    }

    @OnClick(android.R.id.button1)
    void addItem() {
        Grocery grocery = new Grocery(inputField.getText().toString());
        inputField.setText(null);

        //every item needs a unique path
        String path = String.format("/item/%d", grocery.id);
        PutDataMapRequest request = PutDataMapRequest.create(path);
        request.getDataMap().putInt("id", grocery.id);
        request.getDataMap().putString("value", grocery.value);
        PendingResult<DataApi.DataItemResult> result =
                Wearable.DataApi.putDataItem(googleApiClient, request.asPutDataRequest());
        result.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                refreshList();
            }
        });
    }

    private void deleteItem(DataItem dataItem) {
        PendingResult<DataApi.DeleteDataItemsResult> result =
                Wearable.DataApi.deleteDataItems(googleApiClient, dataItem.getUri());

        result.setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
            @Override
            public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
                refreshList();
            }
        });
    }

    private void refreshList() {
        Wearable.DataApi.getDataItems(googleApiClient).setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                groceries.clear();
                for (DataItem dataItem : dataItems) {
                    DataMapItem mapItem = DataMapItem.fromDataItem(dataItem);
                    int id = mapItem.getDataMap().getInt("id");
                    String value = mapItem.getDataMap().getString("value");
                    groceries.add(new Grocery(id, value, dataItem));
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.grocery_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            refreshList();
            return true;
        } else if (item.getItemId() == R.id.action_notify) {
            postNotification();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void postNotification() {
        PendingIntent openListPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, GroceryListActivity.class), 0);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                R.drawable.ic_action_reload, "Reload", openListPendingIntent)
                .build();

        Notification notif = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("WearDemo")
                .setContentText("List Updated")
                .setContentIntent(openListPendingIntent)
                .setAutoCancel(true)
                .extend(new android.support.v4.app.NotificationCompat.WearableExtender().addAction(action))
                .build();

        NotificationManagerCompat mgr = NotificationManagerCompat.from(this);
        mgr.notify(NOTIFICATION_ID, notif);
    }

    private DataApi.DataListener dataListener = new DataApi.DataListener() {
        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            refreshList();
        }
    };

    private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            refreshList();
            // subscribe to updates to the data model
            Wearable.DataApi.addListener(googleApiClient, dataListener);
        }

        @Override
        public void onConnectionSuspended(int i) {
            //NOOP
        }
    };

    private GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            //NOOP
        }
    };

    private RecyclerView.Adapter<ItemViewHolder> adapter = new RecyclerView.Adapter<ItemViewHolder>() {
        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View root = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ItemViewHolder(root);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            final Grocery item = groceries.get(position);
            holder.textView.setText(item.value);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteItem(item.dataItem);
                }
            });
        }

        @Override
        public int getItemCount() {
            return groceries.size();
        }
    };

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

}
