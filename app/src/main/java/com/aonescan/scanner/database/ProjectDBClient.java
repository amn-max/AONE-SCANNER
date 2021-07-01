package com.aonescan.scanner.database;

import android.content.Context;

import androidx.room.Room;

public class ProjectDBClient {
    private static ProjectDBClient mInstance;
    private final Context mCtx;
    private final ProjectDB projectDB;

    private ProjectDBClient(Context mCtx) {
        this.mCtx = mCtx;
        projectDB = Room.databaseBuilder(mCtx, ProjectDB.class, "HistoryPDF")
                .fallbackToDestructiveMigration().build();
    }

    public static synchronized ProjectDBClient getInstance(Context mCtx) {
        if (mInstance == null) {
            mInstance = new ProjectDBClient(mCtx);
        }
        return mInstance;
    }

    public ProjectDB getProjectDB() {
        return projectDB;
    }
}
