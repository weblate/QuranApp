package com.quranapp.android.utils.univ;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static com.quranapp.android.utils.reader.TranslUtils.TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.AVAILABLE_RECITATIONS_FILENAME;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.chapterInfo.ChapterInfoUtils;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.tafsir.TafsirUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Scanner;
import java.util.StringJoiner;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class FileUtils {
    private final Context mContext;

    private FileUtils(@NonNull Context context) {
        mContext = context;
    }

    public static String createPath(String... subPaths) {
        StringJoiner joiner = new StringJoiner(File.separator);
        for (String child : subPaths) {
            if (!TextUtils.isEmpty(child)) {
                joiner.add(child);
            }
        }
        return joiner.toString();
    }

    public static FileUtils newInstance(@NonNull Context context) {
        return new FileUtils(context);
    }

    public File getAppFilesDirectory() {
        return getContext().getFilesDir();
    }

    public File getRecitationDir() {
        return makeAndGetAppResourceDir(RecitationUtils.DIR_NAME);
    }

    public File getRecitationAudioFile(String reciterSlug, int chapterNo, int verseNo) {
        File recitationDir = getRecitationDir();
        String audioSubPathWithReciter = RecitationUtils.prepareAudioPathForSpecificReciter(reciterSlug, chapterNo, verseNo);
        return new File(recitationDir, audioSubPathWithReciter);
    }

    public File getRecitationsManifestFile() {
        File recitationDir = getRecitationDir();
        return new File(recitationDir, AVAILABLE_RECITATIONS_FILENAME);
    }

    public File getTranslationDir() {
        return makeAndGetAppResourceDir(TranslUtils.DIR_NAME);
    }

    public File getSingleTranslationInfoFile(String langCode, String translSlug) {
        File translationDir = getTranslationDir();
        String translInfoSubPathWithLangCode = TranslUtils.prepareTranslInfoPathForSpecificLangNSlug(langCode,
                translSlug);
        return new File(translationDir, translInfoSubPathWithLangCode);
    }

    public File getSingleTranslationFile(int translId, String langCode, String translSlug) {
        File translationDir = getTranslationDir();
        String translSubPathWithLangCode = TranslUtils.prepareTranslPathForSpecificLangNSlug(translId, langCode,
                translSlug);
        return new File(translationDir, translSubPathWithLangCode);
    }

    public File getTranslsManifestFile() {
        File availableDownloads = makeAndGetAppResourceDir(TranslUtils.DIR_NAME_4_AVAILABLE_DOWNLOADS);
        return new File(availableDownloads, TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME);
    }

    public File getChapterInfoDir() {
        return makeAndGetAppResourceDir(ChapterInfoUtils.DIR_NAME);
    }

    public File getChapterInfoFile(String lang, int chapterNo) {
        File chapterInfoDir = getChapterInfoDir();
        String chapterInfoSubPath = ChapterInfoUtils.prepareChapterInfoFilePath(lang, chapterNo);
        return new File(chapterInfoDir, chapterInfoSubPath);
    }

    public File getTafsirDir() {
        return makeAndGetAppResourceDir(TafsirUtils.DIR_NAME);
    }

    public File getTafsirFileSingleVerse(String tafsirSlug, int chapterNo, int verseNo) {
        File tafsirDir = getTafsirDir();
        String tafsirSubPath = TafsirUtils.prepareTafsirFilePathSingleVerse(tafsirSlug, chapterNo, verseNo);
        return new File(tafsirDir, tafsirSubPath);
    }

    public File getTafsirFileFullChapter(String tafsirSlug, int chapterNo) {
        File tafsirDir = getTafsirDir();
        String tafsirSubPath = TafsirUtils.prepareTafsirFilePathFullChapter(tafsirSlug, chapterNo);
        return new File(tafsirDir, tafsirSubPath);
    }

    public File getOtherDirectory() {
        return makeAndGetAppResourceDir(AppUtils.APP_OTHER_DIR);
    }

    public File getAppUpdatesFile() {
        return new File(getOtherDirectory(), "app_updates.json");
    }

    public File makeAndGetAppResourceDir(String resourceDirName) {
        File file = new File(getAppFilesDirectory(), resourceDirName);
        if (file.exists()) return file;

        if (!file.mkdirs()) return null;

        return file;
    }

    public boolean createFile(File file) {
        if (file.exists()) {
            return true;
        }
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
                return file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static File createDirIfNotExists(File directory) {
        if (!directory.exists()) directory.mkdirs();
        return directory;
    }

    public void writeToFile(@NonNull File file, @NonNull String string) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(out);
        osw.write(string);
        osw.close();
    }

    @NonNull
    public String readFile(@NonNull File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;
        while ((str = br.readLine()) != null) sb.append(str);

        br.close();

        return sb.toString();
    }

    public Context getContext() {
        return mContext;
    }

    public void saveBitmap(@NonNull Bitmap bitmap, @NonNull File file) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void deleteDirWithChildren(@NonNull File dir) {
        if (!dir.exists()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) deleteDirWithChildren(child);
            else child.delete();
        }
        dir.delete();
    }

    /**
     * Decodes URI into Bitmap.
     * URI from file can be obtained by using {@link #getFileURI(File)}
     *
     * @param uri The URI from which bitmap is to be decoded
     * @return Returns {@link Bitmap} decoded from the queried URI
     * @throws IOException IOException
     */
    @NonNull
    public Bitmap getBitmapFromUri(@NonNull Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
    }

    /**
     * Instantiates view intent to view an image in default image viewer
     *
     * @param imageUri The image {@link URI}, which is to be viewed
     */
    public void viewImage(@NonNull Uri imageUri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(imageUri, "image/*");
        intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void cloneDirectory(@NonNull File sourceLoc, @NonNull File targetLoc) throws IOException {
        if (sourceLoc.isDirectory()) {
            if (!targetLoc.exists() && !targetLoc.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLoc.getAbsolutePath());
            }

            String[] children = sourceLoc.list();
            if (children != null) {
                for (String child : children) cloneDirectory(new File(sourceLoc, child), new File(targetLoc, child));
            }
        } else {
            File directory = targetLoc.getParentFile();

            if (directory != null) createDirIfNotExists(directory);
            else throw new IOException("Unable to clone directory : " + sourceLoc.getName());

            InputStream in = new FileInputStream(sourceLoc);
            OutputStream out = new FileOutputStream(targetLoc);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        }
    }

    /**
     * Creates {@link URI} of a file.
     *
     * @param file The {@link File} of which URI to be formed.
     * @return Returns the {@link URI} of the file.
     */
    @NonNull
    public Uri getFileURI(@NonNull File file) {
        return FileProvider.getUriForFile(getContext(),
                getContext().getApplicationContext().getPackageName() + ".provider", file);
    }

    @Nullable
    public String getPathFromUri(@NonNull Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }


    public static String readTextFromFile(File textFile) throws FileNotFoundException {
        Scanner in = new Scanner(new FileReader(textFile));
        StringBuilder sb = new StringBuilder();
        while (in.hasNextLine()) {
            sb.append(in.nextLine());
        }
        in.close();

        return sb.toString();
    }


    public static void writeTextIntoFile(File textFile, String stringData) throws Exception {
        PrintWriter out = new PrintWriter(textFile);
        out.println(stringData);
        out.flush();
        out.close();
    }

    public interface FileCallback {
        void onSuccess();

        void onFailed(@NonNull Exception e);
    }
}
