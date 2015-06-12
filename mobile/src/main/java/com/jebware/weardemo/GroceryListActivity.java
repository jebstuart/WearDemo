package com.jebware.weardemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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

    //TODO comments
    //TODO menu option to clear everything in the data

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
        Wearable.DataApi.putDataItem(googleApiClient, request.asPutDataRequest()).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                refreshList();
            }
        });
    }

    private void deleteItem(DataItem dataItem) {
        Wearable.DataApi.deleteDataItems(googleApiClient, dataItem.getUri()).setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
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
