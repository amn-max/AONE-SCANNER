package com.aonescan.scanner.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface ProjectDao {
    @Query("SELECT * FROM project WHERE imagePaths !=:s  ORDER BY createdOn DESC")
    LiveData<List<Project>> getAllProjects(String s);

    @Query("SELECT * FROM project ORDER BY createdOn DESC LIMIT 1")
    LiveData<Project> getLatestProject();

    @Query("SELECT * FROM project ORDER BY createdOn DESC LIMIT 1")
    Project getLatestProjectStatic();

    @Query("SELECT * FROM project WHERE id = :historyId ORDER BY createdOn DESC")
    LiveData<Project> getProjectById(int historyId);

    @Query("UPDATE project set projectName = :projectName WHERE id = :historyId")
    void updateProjectName(String projectName, int historyId);

    @Insert(onConflict = REPLACE)
    long insert(Project project);

    @Query("DELETE FROM project WHERE imagePaths =:s")
    void deleteEmptyProjects(String s);

    @Query("DELETE FROM project WHERE id = :historyId")
    void delete(int historyId);

    @Delete
    void delete(Project project);

    @Query("UPDATE project set imagePaths = :paths WHERE id = :historyId")
    void update(int historyId, ArrayList<String> paths);
}
