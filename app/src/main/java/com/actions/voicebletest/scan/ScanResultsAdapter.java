package com.actions.voicebletest.scan;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actions.voicebletest.R;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

//import butterknife.BindView;
//import butterknife.ButterKnife;

class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.text_device_name)
        TextView textDeviceName;
        @BindView(R.id.text_rssi)
        TextView textRssi;
        @BindView(R.id.text_mac_address)
        TextView textMacAddress;
        @BindView(R.id.btn_connect)
        Button btnConnect;
        @BindView(R.id.view_icon)
        ImageView viewIcon;
        @BindView(R.id.more_option)
        ImageView moreOption;
        @BindView(R.id.text_bond_state)
        TextView textBondState;

        ViewHolder(View itemView, View.OnClickListener onClickListener, View.OnClickListener onMenuClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.onClick(itemView);
                }
            });
            moreOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onMenuClickListener.onClick(itemView);
                }
            });
        }
    }

    interface OnAdapterItemClickListener {

        void onAdapterViewClick(View view);

        void onMenuAdapterViewClick(View view);
    }

    private static final Comparator<ScanResult> SORTING_COMPARATOR = (lhs, rhs) ->
            lhs.getBleDevice().getMacAddress().compareTo(rhs.getBleDevice().getMacAddress());
    private final List<ScanResult> data = new ArrayList<>();
    private OnAdapterItemClickListener onAdapterItemClickListener;
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (onAdapterItemClickListener != null) {
                onAdapterItemClickListener.onAdapterViewClick(v);
            }
        }
    };

    private final View.OnClickListener onMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (onAdapterItemClickListener != null) {
                onAdapterItemClickListener.onMenuAdapterViewClick(v);
            }
        }
    };

    void addScanResult(ScanResult bleScanResult) {
        // Not the best way to ensure distinct devices, just for sake on the demo.

        for (int i = 0; i < data.size(); i++) {

            if (data.get(i).getBleDevice().equals(bleScanResult.getBleDevice())) {
                data.set(i, bleScanResult);
                notifyItemChanged(i);
                return;
            }
        }

        data.add(bleScanResult);
        Collections.sort(data, SORTING_COMPARATOR);
        notifyDataSetChanged();
    }

    void clearScanResults() {
        data.clear();
        notifyDataSetChanged();
    }

    ScanResult getItemAtPosition(int childAdapterPosition) {
        return data.get(childAdapterPosition);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ScanResult rxBleScanResult = data.get(position);
        final RxBleDevice bleDevice = rxBleScanResult.getBleDevice();
        int bondState = bleDevice.getBluetoothDevice().getBondState();

        holder.textDeviceName.setText(bleDevice.getName()== null ? "Null" : bleDevice.getName());
        holder.textMacAddress.setText(bleDevice.getMacAddress());
        holder.textRssi.setText(rxBleScanResult.getRssi() + "");
        if (bondState == BluetoothDevice.BOND_NONE) {
            holder.textBondState.setText(R.string.not_bond_state);
        } else if (bondState == BluetoothDevice.BOND_BONDED){
            holder.textBondState.setText(R.string.bonded_state);
        } else if (bondState == BluetoothDevice.BOND_BONDING){
            holder.textBondState.setText(R.string.bonding_state);
        }
        if (bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED){
            holder.btnConnect.setText(R.string.open);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_result, parent, false);
        return new ViewHolder(itemView, onClickListener, onMenuClickListener);
    }

    void setOnAdapterItemClickListener(OnAdapterItemClickListener onAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener;
    }
}
