package com.example.basededatos_sqlite;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.example.basededatos_sqlite.R;
import basededatos.DatabaseHelper;
import basededatos_modelo.Note;
import utils.MyDiviterItemDecoration;
import utils.RecyclerTouchListener;
import virtualizacion.NotesAdapter;


public class MainActivity extends AppCompatActivity {
    private NotesAdapter mAdapter;
    private List<Note> notesList = new ArrayList<>();
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private TextView noNotesView;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        recyclerView = findViewById(R.id.recycler_view);
        noNotesView = findViewById(R.id.empty_notes_view);

        db = new DatabaseHelper(this);

        notesList.addAll(db.getAllNotes());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNoteDialog(false,null,-1);
            }
        });

        mAdapter = new NotesAdapter(this, notesList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDiviterItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        toggleEmptyNotes();

        /**
         *  Almantener pulsado el elemento RecyclerView, se abre el cuadro de dialogo de alerta
         * con opciones para elegir
         * Editar y Eliminar
         * **/

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
            }

            @Override
            public void onLongClick(View view, int position) {
                showActionsDialog(position);
            }
        }));
    }

    //Insertar una nueva nota en db y refrescando la lista

    private void createNote(String note) {
        //insertando nota en db
        //id nota recien insertada
       long id = db.insertNote(note);
        //obtener la nota recion insertada de db
        Note n = db.getNote(id);

        if (n != null) {
            //añade una nueva nota a la lista de matrices en la posicion 0
            notesList.add(0, n);
            //actualizar la lista
            mAdapter.notifyDataSetChanged();

            toggleEmptyNotes();
        }
    }
    /**
     * Actualizacion de nota en db y actualizacion
     * item en la lista por su posicion
     **/
    private void updateNote(String note, int position) {
        Note n = notesList.get(position);
        n.setNote(note);

        db.updateNote(n);

        notesList.set(position, n);
        mAdapter.notifyItemChanged(position);

        toggleEmptyNotes();
    }
    /*
    * Eliminando nota de sqlite y eliminando el item de la lista por su posicion
    * */
    private void deleteNote(int position) {
        //borrando la nota de la db
        db.deleteNote(notesList.get(position));
//eliminando la nota de la lista
        notesList.remove(position);
        mAdapter.notifyItemRemoved(position);

        toggleEmptyNotes();
    }
    /**
     * Abre el dialogo con editar-eliminar opciones
     * edita-0
     * eliminar-0
     * **/
    private void showActionsDialog(final int position) {
        CharSequence colors[] = new CharSequence[]{"Editar", "Eliminar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona opción");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showNoteDialog(true, notesList.get(position), position);
                } else {
                    deleteNote(position);
                }
            }
        });
        builder.show();
    }

    /**
     * Muestra en dialogo de alerta con las opciones de editar texto para ingresar / editar una nota
     * cuando shouldUpdate =  true, muestra automaticamente la nota antigua y cambia el boton de texto
     * para ACTUALIZAR
     * **/

    private void showNoteDialog(final boolean shouldUpdate, final Note note, final int position) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view = layoutInflaterAndroid.inflate(R.layout.note_dialog, null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilderUserInput.setView(view);

        final EditText inputNote = view.findViewById(R.id.note);
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        dialogTitle.setText(!shouldUpdate ? getString(R.string.lbl_new_note_title) : getString(R.string.lbl_edit_note_title));

        if (shouldUpdate && note != null) {
            inputNote.setText(note.getNote());
        }
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(shouldUpdate ? "Actualizar" : "Grabar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {

                    }
                })
                .setNegativeButton("cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });

        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(inputNote.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Enter nota!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alertDialog.dismiss();
                }
                //comprobar si el usuario eesta actualizando nota
                if (shouldUpdate && note != null) {
                    updateNote(inputNote.getText().toString(), position);
                } else {
                    //crear nueva nota
                    createNote(inputNote.getText().toString());
                }
            }
        });
    }

    /**
     * Lista de alternar y ver notas vacias
     * **/
    private void toggleEmptyNotes() {
        if (db.getNotesCount() > 0) {
            noNotesView.setVisibility(View.GONE);
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }
}