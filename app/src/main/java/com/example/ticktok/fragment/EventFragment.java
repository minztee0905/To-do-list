package com.example.ticktok.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.adapter.EventAdapter;
import com.example.ticktok.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventFragment extends Fragment {

    private EventAdapter eventAdapter;
    private View layoutEmptyEvents;
    private ListenerRegistration eventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvEvents = view.findViewById(R.id.rvEvents);
        layoutEmptyEvents = view.findViewById(R.id.layoutEmptyEvents);

        eventAdapter = new EventAdapter();
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(eventAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        startEventListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopEventListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopEventListener();
        eventAdapter = null;
        layoutEmptyEvents = null;
    }

    private void startEventListener() {
        if (!isAdded() || eventAdapter == null) {
            return;
        }

        stopEventListener();
        eventListener = FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("targetDate", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded() || eventAdapter == null) {
                        return;
                    }
                    if (error != null || snapshot == null) {
                        showEmptyState(true);
                        return;
                    }

                    List<Event> events = mapSnapshotToEvents(snapshot);
                    eventAdapter.submitList(events);
                    showEmptyState(events.isEmpty());
                });
    }

    private void stopEventListener() {
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    @NonNull
    private List<Event> mapSnapshotToEvents(@NonNull QuerySnapshot snapshot) {
        List<Event> events = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            Event event = doc.toObject(Event.class);
            event.setId(doc.getId());
            events.add(event);
        }
        return events;
    }

    private void showEmptyState(boolean show) {
        if (layoutEmptyEvents != null) {
            layoutEmptyEvents.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
