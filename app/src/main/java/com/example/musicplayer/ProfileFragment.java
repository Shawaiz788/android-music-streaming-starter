package com.example.musicplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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


public class ProfileFragment extends Fragment {

    ConstraintLayout btnQuit;
    FirebaseAuth auth;
    FirebaseUser user;

    ImageView btnClose, btnEditProfile, ivProfilePic;
    TextView profileName, profileEmail;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MyApplication.OnUserLoadedListener userListener;

    public ProfileFragment() {
        // Required empty public constructor
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
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            String base64Image = encodeImageToBase64(bitmap);
            if (base64Image != null) {
                if (MyApplication.userHandler != null) {
                    MyApplication.userHandler.setProfileImageUrl(base64Image);
                }
                Toast.makeText(requireContext(), "Image updated!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
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
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (MyApplication.userHandler != null) {
                MyApplication.userHandler.setName(name);
                Toast.makeText(requireContext(), "Updating...", Toast.LENGTH_SHORT).show();
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
