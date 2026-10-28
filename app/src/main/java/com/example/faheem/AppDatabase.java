package com.example.faheem;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Pet.class, User.class, AnalysisResult.class},
          version = 6, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PetDao petDao();
    public abstract UserDao userDao();
    public abstract AnalysisResultDao analysisResultDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(4);
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE users ADD COLUMN passwordHash TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN verified INTEGER NOT NULL DEFAULT 0");
        }
    };
    /* Versions 4–6 did not introduce columns or tables. This preserves valid
       existing records while normalising plan values introduced by those releases. */
    private static final Migration MIGRATION_3_6 = new Migration(3, 6) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE users SET planType = 'free' WHERE planType IS NULL OR planType = ''");
            database.execSQL("UPDATE users SET lastAnalysisDate = '' WHERE lastAnalysisDate IS NULL");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "faheem_database"
                            ).addMigrations(MIGRATION_2_3, MIGRATION_3_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
