package com.example.friendmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"Convert2MethodRef", "RedundantSuppression", "SpellCheckingInspection", "UnusedPRM"})
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity_GPS";
    GoogleMap mMap;
    FusedLocationProviderClient fusedLocationClient;
    LocationCallback locationCallback;
    DatabaseReference locationRef;
    FirebaseAuth mAuth;
    BottomNavigationView bottomNav;
    FirebaseFirestore firestore;

    FloatingActionButton fabDirections;
    LatLng selectedFriendLatLng = null;

    final HashMap<String, Marker> friendMarkers = new HashMap<>();
    final List<String> friendIds = new ArrayList<>();
    final HashMap<String, String> friendNamesCache = new HashMap<>();

    private ListenerRegistration firestoreListener;
    private final List<ValueEventListener> realtimeListeners = new ArrayList<>();
    private final List<DatabaseReference> listenedRefs = new ArrayList<>();

    static final int LOCATION_PERMISSION_REQUEST = 1001;
    static final int NOTIFICATION_PERMISSION_REQUEST = 1002;
    private boolean isFirstZoom = true;

    // ĐÃ SỬA DỨT ĐIỂM SẬP APP: Chuyển kiểu dữ liệu sang CardView để đồng bộ hoàn toàn với layout XML
    CardView layoutMarkerEmojiTease;
    String selectedFriendId = null;
    DatabaseReference teaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRef = FirebaseDatabase.getInstance("https://friendmap-53fe9-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("locations");
        teaseRef = FirebaseDatabase.getInstance("https://friendmap-53fe9-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("teases");
        firestore = FirebaseFirestore.getInstance();

        fabDirections = findViewById(R.id.fabDirections);
        layoutMarkerEmojiTease = findViewById(R.id.layoutMarkerEmojiTease);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.fabMyLocation).setOnClickListener(v -> moveToMyLocation());

        if (fabDirections != null) {
            fabDirections.setOnClickListener(v -> {
                if (selectedFriendLatLng != null) {
                    String mapUri = "google.navigation:q=" + selectedFriendLatLng.latitude + "," + selectedFriendLatLng.longitude + "&mode=d";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri));
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        String webUri = "https://www.google.com/maps/dir/?api=1&destination=" + selectedFriendLatLng.latitude + "," + selectedFriendLatLng.longitude;
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
                    }
                }
            });
        }

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                return true;
            }

            Intent intent = null;
            if (id == R.id.nav_friends) {
                intent = new Intent(this, FriendsActivity.class);
            } else if (id == R.id.nav_chat) {
                intent = new Intent(this, ChatActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });

        checkAndRequestNotificationPermission();
        listenForIncomingTeases();
        setupTeaseEmojiClickListeners();
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception e) {
            Log.e(TAG, "Lỗi nạp style bản đồ: " + e.getMessage());
        }

        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setOnMarkerClickListener(marker -> {
            selectedFriendLatLng = marker.getPosition();

            for (Map.Entry<String, Marker> entry : friendMarkers.entrySet()) {
                if (entry.getValue().getId().equals(marker.getId())) {
                    selectedFriendId = entry.getKey();
                    break;
                }
            }

            if (fabDirections != null) {
                fabDirections.setVisibility(View.VISIBLE);
            }
            if (layoutMarkerEmojiTease != null && selectedFriendId != null) {
                layoutMarkerEmojiTease.setVisibility(View.VISIBLE);
            }
            marker.showInfoWindow();
            return false;
        });

        mMap.setOnMapClickListener(latLng -> {
            if (fabDirections != null) {
                fabDirections.setVisibility(View.GONE);
                selectedFriendLatLng = null;
            }
            if (layoutMarkerEmojiTease != null) {
                layoutMarkerEmojiTease.setVisibility(View.GONE);
                selectedFriendId = null;
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }

        loadFriendsAndShowOnMap();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);

        com.google.android.gms.location.LocationRequest locationRequest = new com.google.android.gms.location.LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocationToFirebase(location);

                    if (isFirstZoom) {
                        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                        isFirstZoom = false;
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void updateLocationToFirebase(Location location) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());
        locationData.put("isOnline", true);

        locationRef.child(uid).setValue(locationData)
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi đẩy định vị lên Realtime: " + e.getMessage()));
    }

    private void moveToMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 16f));
            }
        });
    }

    private void clearRealtimeListenersAnToan() {
        if (realtimeListeners != null && listenedRefs != null) {
            int listenerCount = Math.min(realtimeListeners.size(), listenedRefs.size());
            for (int i = listenerCount - 1; i >= 0; i--) {
                try {
                    if (listenedRefs.get(i) != null && realtimeListeners.get(i) != null) {
                        listenedRefs.get(i).removeEventListener(realtimeListeners.get(i));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi giải phóng tại vị trí: " + i);
                }
            }
            realtimeListeners.clear();
            listenedRefs.clear();
        }
    }

    private void loadFriendsAndShowOnMap() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUid = mAuth.getCurrentUser().getUid();

        firestoreListener = firestore.collection("friendRequests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe dữ liệu bạn bè từ Firestore: ", error);
                        return;
                    }
                    if (value == null) return;

                    clearRealtimeListenersAnToan();
                    friendIds.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String receiverId = doc.getString("receiverId");

                        if (currentUid.equals(senderId) && receiverId != null) {
                            friendIds.add(receiverId);
                        } else if (currentUid.equals(receiverId) && senderId != null) {
                            friendIds.add(senderId);
                        }
                    }

                    Log.d(TAG, "Tìm thấy " + friendIds.size() + " người bạn đã liên kết thành công.");

                    for (String friendId : friendIds) {
                        DatabaseReference friendLocationRef = locationRef.child(friendId);

                        ValueEventListener vListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) return;

                                Object latObj = snapshot.child("latitude").getValue();
                                Object lngObj = snapshot.child("longitude").getValue();
                                if (latObj == null || lngObj == null) return;

                                double lat = Double.parseDouble(latObj.toString());
                                double lng = Double.parseDouble(lngObj.toString());
                                LatLng friendLatLng = new LatLng(lat, lng);

                                String name = friendNamesCache.get(friendId);
                                if (name != null) {
                                    updateMarkerOnMap(friendId, friendLatLng, name);
                                } else {
                                    firestore.collection("users").document(friendId).get()
                                            .addOnSuccessListener(userDoc -> {
                                                String finalName = "Bạn bè";
                                                if (userDoc.exists()) {
                                                    String hoTen = userDoc.getString("hoTen");
                                                    String displayName = userDoc.getString("displayName");
                                                    String username = userDoc.getString("username");

                                                    if (hoTen != null && !hoTen.isEmpty()) finalName = hoTen;
                                                    else if (displayName != null && !displayName.isEmpty()) finalName = displayName;
                                                    else if (username != null && !username.isEmpty()) finalName = username;
                                                }
                                                friendNamesCache.put(friendId, finalName);
                                                updateMarkerOnMap(friendId, friendLatLng, finalName);
                                            })
                                            .addOnFailureListener(e -> updateMarkerOnMap(friendId, friendLatLng, "Bạn bè"));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Lỗi kết nối Realtime DB: " + error.getMessage());
                            }
                        };

                        friendLocationRef.addValueEventListener(vListener);
                        listenedRefs.add(friendLocationRef);
                        realtimeListeners.add(vListener);
                    }
                });
    }

    private void updateMarkerOnMap(String friendId, LatLng latLng, String title) {
        if (mMap == null) return;

        String avatarUrl = "https://ui-avatars.com/api/?name=" + Uri.encode(title) + "&background=random&size=128&rounded=true";

        runOnUiThread(() -> {
            if (friendMarkers.containsKey(friendId)) {
                Marker marker = friendMarkers.get(friendId);
                if (marker != null) {
                    marker.setPosition(latLng);
                    marker.setTitle(title);
                    if (marker.isInfoWindowShown()) {
                        marker.showInfoWindow();
                    }
                }
            } else {
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(title));
                if (marker != null) {
                    friendMarkers.put(friendId, marker);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

                    Glide.with(getApplicationContext())
                            .asBitmap()
                            .load(avatarUrl)
                            .override(120, 120)
                            .circleCrop()
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    if (friendMarkers.containsKey(friendId)) {
                                        Marker targetMarker = friendMarkers.get(friendId);
                                        if (targetMarker != null) {
                                            targetMarker.setIcon(BitmapDescriptorFactory.fromBitmap(resource));
                                        }
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                }
            }
        });
    }

    private void setupTeaseEmojiClickListeners() {
        int[] emojiButtonIds = {R.id.btnTeaseFire, R.id.btnTeaseLaugh, R.id.btnTeaseGhost, R.id.btnTeasePoop};
        String[] emojis = {"🔥", "😂", "👻", "💩"};

        for (int i = 0; i < emojiButtonIds.length; i++) {
            final String emoji = emojis[i];
            View btn = findViewById(emojiButtonIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    if (selectedFriendId == null || mAuth.getCurrentUser() == null) return;

                    String myUid = mAuth.getCurrentUser().getUid();

                    Map<String, Object> teaseData = new HashMap<>();
                    teaseData.put("fromUid", myUid);
                    teaseData.put("emoji", emoji);
                    teaseData.put("timestamp", System.currentTimeMillis());

                    teaseRef.child(selectedFriendId).setValue(teaseData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "Đã thả " + emoji + " trêu bạn!", Toast.LENGTH_SHORT).show();
                                if (layoutMarkerEmojiTease != null) layoutMarkerEmojiTease.setVisibility(View.GONE);
                            });
                });
            }
        }
    }

    private void listenForIncomingTeases() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        teaseRef.child(myUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String fromUid = snapshot.child("fromUid").getValue(String.class);
                String emoji = snapshot.child("emoji").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                if (timestamp != null && (System.currentTimeMillis() - timestamp < 5000)) {
                    String senderName = friendNamesCache.get(fromUid);
                    if (senderName == null) senderName = "Một người bạn";

                    Toast.makeText(MainActivity.this, "⚡ " + senderName + " vừa thả " + emoji + " trêu chọc bạn trên bản đồ!", Toast.LENGTH_LONG).show();

                    teaseRef.child(myUid).removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi lắng nghe trêu chọc: " + error.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Cần quyền vị trí để hiển thị bản đồ!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Người dùng đã cấp quyền thông báo đẩy thành công.");
            } else {
                Toast.makeText(this, "Bạn sẽ không nhận được thông báo tin nhắn mới khi tắt ứng dụng!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mAuth.getCurrentUser() != null && locationRef != null) {
            String uid = mAuth.getCurrentUser().getUid();
            locationRef.child(uid).child("isOnline").setValue(false);
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        clearRealtimeListenersAnToan();
        super.onDestroy();
    }
}