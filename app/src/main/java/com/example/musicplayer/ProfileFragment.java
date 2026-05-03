package com.example.musicplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class ProfileFragment extends Fragment {

    ConstraintLayout btnQuit;
    FirebaseAuth auth;
    FirebaseUser user;

    ImageView btnClose, btnEditProfile, ivProfilePic;
    TextView profileName, profileEmail;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MyApplication.OnUserLoadedListener userListener;

    private Map<String, View> themeViews = new HashMap<>();

    public ProfileFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        handleImageSelection(uri);
                    }
                }
        );
    }

    private void handleImageSelection(Uri uri) {
        try {
            Bitmap bitmap;
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(inputStream);
            }

            if (bitmap == null) return;

            bitmap = rotateImageIfRequired(bitmap, uri);

            Toast.makeText(requireContext(), getString(R.string.uploading_to_drive), Toast.LENGTH_SHORT).show();

            String fileName = "profile_" + (user != null ? user.getUid() : "unknown");

            DriveUploader.uploadImage(bitmap, fileName, new DriveUploader.UploadCallback() {
                @Override
                public void onSuccess(String directLink) {
                    requireActivity().runOnUiThread(() -> {
                        if (MyApplication.userHandler != null) {
                            MyApplication.userHandler.setProfileImageUrl(directLink);
                            Toast.makeText(requireContext(), getString(R.string.profile_picture_updated), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), getString(R.string.upload_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.failed_to_process_image), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) {
        try (InputStream input = requireContext().getContentResolver().openInputStream(selectedImage)) {
            if (input == null) return img;
            androidx.exifinterface.media.ExifInterface ei = new androidx.exifinterface.media.ExifInterface(input);

            int orientation = ei.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (Exception e) {
            return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        int maxWidth = 300;
        int maxHeight = 300;
        float scale = Math.min(((float) maxWidth / bitmap.getWidth()), ((float) maxHeight / bitmap.getHeight()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] b = baos.toByteArray();
        return "data:image/jpeg;base64," + Base64.encodeToString(b, Base64.DEFAULT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnQuit = view.findViewById(R.id.btnQuit);
        btnClose = view.findViewById(R.id.closeBtn);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        userListener = this::updateUI;
        MyApplication.subscribeUser(userListener);

        themeViews.put("teal", view.findViewById(R.id.colorTeal));
        themeViews.put("orange", view.findViewById(R.id.colorOrange));
        themeViews.put("purple", view.findViewById(R.id.colorPurple));
        themeViews.put("blue", view.findViewById(R.id.colorBlue));
        themeViews.put("red", view.findViewById(R.id.colorRed));
        themeViews.put("black", view.findViewById(R.id.colorBlack));

        for (Map.Entry<String, View> entry : themeViews.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().setOnClickListener(v -> updateTheme(entry.getKey(), view));
            }
        }

        String currentTheme = ThemeHelper.getTheme(requireContext());
        highlightSelectedTheme(currentTheme);

        btnEditProfile.setOnClickListener((v) -> showEditProfileDialog());

        btnClose.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(ProfileFragment.this);
            navController.navigate(R.id.homeFragment);
        });

        btnQuit.setOnClickListener((v) -> {
            auth.signOut();
            Intent i = new Intent(requireContext(), SignInSignup.class);
            startActivity(i);
            requireActivity().finish();
        });
    }

    private void highlightSelectedTheme(String selectedTheme) {
        for (Map.Entry<String, View> entry : themeViews.entrySet()) {
            View view = entry.getValue();
            if (view == null) continue;

            if (entry.getKey().equals(selectedTheme)) {
                view.setScaleX(1.2f);
                view.setScaleY(1.2f);
                view.setAlpha(1.0f);
            } else {
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
                view.setAlpha(0.6f);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            MyApplication.unsubscribeUser(userListener);
        }
    }

    private void updateUI(User userInfo) {
        if (userInfo != null && isAdded()) {
            profileName.setText(userInfo.getName());
            profileEmail.setText(userInfo.getEmail());
            String imageUrl = userInfo.getProfileImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.icon_pfp)
                        .error(R.drawable.icon_pfp)
                        .into(ivProfilePic);
            }
        }
    }

    private void updateTheme(String themeName, View view) {
        ThemeHelper.saveTheme(requireContext(), themeName);
        highlightSelectedTheme(themeName);
        Toast.makeText(requireContext(), getString(R.string.theme_changed_to, themeName), Toast.LENGTH_SHORT).show();
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        View btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnSave = dialogView.findViewById(R.id.btnSave);

        if (MyApplication.currentUserInfo != null) {
            etName.setText(MyApplication.currentUserInfo.getName());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.name_cannot_be_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            if (MyApplication.userHandler != null) {
                MyApplication.userHandler.setName(name);
                Toast.makeText(requireContext(), getString(R.string.updating), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
}
