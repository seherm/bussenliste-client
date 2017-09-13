package com.hermann.bussenliste;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;

import java.util.ArrayList;

public class PlayerDetailsActivity extends AppCompatActivity {

    private ArrayList selectedItems;
    private Player selectedPlayer;
    private FinesAdapter finesAdapter;
    private DataSourcePlayer dataSourcePlayer;
    private TextView totalSumOfFines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_details);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        dataSourcePlayer = new DataSourcePlayer(this);

        selectedPlayer = (Player) getIntent().getSerializableExtra("SelectedPlayer");
        TextView playerName = (TextView) findViewById(R.id.player_name);
        playerName.setText(selectedPlayer.getName());
        totalSumOfFines = (TextView) findViewById(R.id.total_sum_of_fines);
        totalSumOfFines.setText(Integer.toString(selectedPlayer.getTotalSumOfFines()) + " CHF");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFineSelectionDialog();
            }
        });

        final ListView finesListView = (ListView) findViewById(R.id.finesListView);
        finesAdapter = new FinesAdapter(this, selectedPlayer.getFines());
        finesListView.setAdapter(finesAdapter);
        finesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        finesListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                int checkedCount = finesListView.getCheckedItemCount();
                actionMode.setTitle(Integer.toString(checkedCount));
                finesAdapter.toggleSelection(i);
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.delete_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.delete_mode:
                        SparseBooleanArray selected = finesAdapter.getSelectedIds();
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                Fine selectedItem = finesAdapter.getItem(selected.keyAt(i));
                                finesAdapter.remove(selectedItem);
                                selectedPlayer.getFines().remove(selectedItem);
                            }
                        }
                        totalSumOfFines.setText(Integer.toString(selectedPlayer.getTotalSumOfFines()) + " CHF");
                        dataSourcePlayer.open();
                        try {
                            dataSourcePlayer.updatePlayer(selectedPlayer.getId(),selectedPlayer.getFines());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        dataSourcePlayer.close();
                        actionMode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
    }

    public void showFineSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        selectedItems = new ArrayList<>();
        dataSourcePlayer.open();
        builder.setTitle(R.string.add_fair)

                .setMultiChoiceItems(R.array.fines, null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selectedItems.add(which);
                                } else if (selectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    selectedItems.remove(Integer.valueOf(which));
                                }
                            }
                        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                for (Object fine : selectedItems) {
                    if (fine.equals(0)) {
                        selectedPlayer.addFine(FineType.LATE_AT_THE_GAME);
                        finesAdapter.notifyDataSetChanged();
                        totalSumOfFines.setText(Integer.toString(selectedPlayer.getTotalSumOfFines()) + " CHF");
                    } else if (fine.equals(1)) {
                        selectedPlayer.addFine(FineType.LATE_IN_TRAINING);
                        finesAdapter.notifyDataSetChanged();
                        totalSumOfFines.setText(Integer.toString(selectedPlayer.getTotalSumOfFines()) + " CHF");
                    } else {
                        break;
                    }
                }

                try {
                    dataSourcePlayer.updatePlayer(selectedPlayer.getId(), selectedPlayer.getFines());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
