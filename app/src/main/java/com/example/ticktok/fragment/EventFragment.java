package com.example.ticktok.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.adapter.EventAdapter;
import com.example.ticktok.model.Event;
import com.example.ticktok.reminder.EventReminderManager;
import com.example.ticktok.reminder.ReminderManager;
import com.example.ticktok.util.UserFirestorePaths;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventFragment extends Fragment {

    private EventAdapter eventAdapter;
    private View layoutEmptyEvents;
    private ListenerRegistration eventListener;

    private static final String TAG_EDIT_EVENT_SHEET = "edit_event_sheet";

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

        eventAdapter = new EventAdapter(this::onEventLongPressed);
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(eventAdapter);
    }

    private void onEventLongPressed(@NonNull View anchorView, @NonNull Event event) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_event_actions, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> handleEventAction(item, event));
        popupMenu.show();
    }

    private boolean handleEventAction(@NonNull MenuItem item, @NonNull Event event) {
        int id = item.getItemId();
        if (id == R.id.action_event_edit) {
            openEditEventSheet(event);
            return true;
        }
        if (id == R.id.action_event_delete) {
            confirmDeleteEvent(event);
            return true;
        }
        return false;
    }

    private void openEditEventSheet(@NonNull Event event) {
        if (!isAdded()) {
            return;
        }
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.delete_event_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (requireActivity().getSupportFragmentManager().findFragmentByTag(TAG_EDIT_EVENT_SHEET) != null) {
            return;
        }

        AddEventBottomSheetFragment sheet = AddEventBottomSheetFragment.newInstanceForEdit(
                event.getId(),
                event.getTitle(),
                event.getIcon(),
                event.getTargetDate()
        );
        sheet.show(requireActivity().getSupportFragmentManager(), TAG_EDIT_EVENT_SHEET);
    }

    private void confirmDeleteEvent(@NonNull Event event) {
        String title = event.getTitle() == null ? "" : event.getTitle();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_event_title)
                .setMessage(getString(R.string.delete_event_message, title))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteEvent(event))
                .show();
    }

    private void deleteEvent(@NonNull Event event) {
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.delete_event_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference eventsRef = UserFirestorePaths.getUserCollection("events");
        if (eventsRef == null) {
            Toast.makeText(requireContext(), R.string.auth_error_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        eventsRef.document(event.getId().trim())
                .delete()
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        EventReminderManager.cancelEventReminders(requireContext(), event.getId().trim());
                        Toast.makeText(requireContext(), R.string.delete_event_success, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.delete_event_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        ReminderManager.ensureNotificationPermission(requireActivity());
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

        CollectionReference eventsRef = UserFirestorePaths.getUserCollection("events");
        if (eventsRef == null) {
            eventAdapter.submitList(new ArrayList<>());
            showEmptyState(true);
            return;
        }

        stopEventListener();
        eventListener = eventsRef
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


                    for (Event event : events) {
                        EventReminderManager.setEventReminders(requireContext(), event);
                    }
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
