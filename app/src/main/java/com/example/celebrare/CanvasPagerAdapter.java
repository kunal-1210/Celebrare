package com.example.celebrare;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CanvasPagerAdapter extends FragmentStateAdapter {

    private List<CanvasData> canvasDataList;
    private final HashMap<Long, CanvasFragment> fragmentMap = new HashMap<>();

    public CanvasPagerAdapter(@NonNull FragmentActivity fa, List<CanvasData> initialData) {
        super(fa);
        canvasDataList = initialData;
        for (CanvasData data : initialData) {
            fragmentMap.put(data.getId(), CanvasFragment.newInstance(data));
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        CanvasData data = canvasDataList.get(position);
        long id = data.getId();
        CanvasFragment fragment = fragmentMap.get(id);
        if (fragment == null) {
            fragment = CanvasFragment.newInstance(data);
            fragmentMap.put(id, fragment);
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return canvasDataList.size();
    }

    @Override
    public long getItemId(int position) {
        return canvasDataList.get(position).getId();
    }

    @Override
    public boolean containsItem(long itemId) {
        for (CanvasData data : canvasDataList) {
            if (data.getId() == itemId) return true;
        }
        return false;
    }

    public CanvasFragment getFragmentById(long id) {
        return fragmentMap.get(id);
    }

    // Add new sheet
    public void addCanvas(CanvasData data) {
        canvasDataList.add(data);
        fragmentMap.put(data.getId(), CanvasFragment.newInstance(data));
        notifyItemInserted(canvasDataList.size() - 1);
    }

// Remove sheet safely
    public void removeCanvas(int position) {
        if (position < 0 || position >= canvasDataList.size()) return;

        CanvasData data = canvasDataList.get(position);

        // Remove from fragment map first
        fragmentMap.remove(data.getId());

        // Remove from data list
        canvasDataList.remove(position);

        // Notify adapter
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, canvasDataList.size() - position); // recalc positions

    }

    // Update sheet list completely
    public void setCanvasList(List<CanvasData> newList) {
        this.canvasDataList = newList;
        // Update fragmentMap: add missing fragments
        for (CanvasData data : newList) {
            fragmentMap.putIfAbsent(data.getId(), CanvasFragment.newInstance(data));
        }
        notifyDataSetChanged();
    }
    public void addSheet(CanvasData sheet) {
        canvasDataList.add(sheet);
        notifyItemInserted(canvasDataList.size() - 1);
    }


}
