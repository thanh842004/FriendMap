package com.example.friendmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Base64;

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
    final HashMap<String, Circle> friendCircles = new HashMap<>();
    final HashMap<String, Boolean> friendOnlineStatus = new HashMap<>();
    final HashMap<String, Handler> pulseHandlers = new HashMap<>();
    final HashMap<String, Runnable> pulseRunnables = new HashMap<>();

    final List<String> friendIds = new ArrayList<>();
    final HashMap<String, String> friendNamesCache = new HashMap<>();
    final HashMap<String, String> friendAvatarCache = new HashMap<>();

    private ListenerRegistration firestoreListener;
    private final List<ValueEventListener> realtimeListeners = new ArrayList<>();
    private final List<DatabaseReference> listenedRefs = new ArrayList<>();

    static final int LOCATION_PERMISSION_REQUEST = 1001;
    static final int NOTIFICATION_PERMISSION_REQUEST = 1002;
    private boolean isFirstZoom = true;

    CardView layoutMarkerEmojiTease;
    String selectedFriendId = null;
    DatabaseReference teaseRef;

    EditText etQuickMessage;
    ImageButton btnSendQuickMessage;

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
        etQuickMessage = findViewById(R.id.etQuickMessage);
        btnSendQuickMessage = findViewById(R.id.btnSendQuickMessage);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.fabMyLocation).setOnClickListener(v -> moveToMyLocation());

        if (fabDirections != null) {
            fabDirections.setOnClickListener(v -> {
                if (selectedFriendLatLng != null) {
                    String mapUri = "google.navigation:q=" + selectedFriendLatLng.latitude
                            + "," + selectedFriendLatLng.longitude + "&mode=d";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri));
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        String webUri = "https://www.google.com/maps/dir/?api=1&destination="
                                + selectedFriendLatLng.latitude + "," + selectedFriendLatLng.longitude;
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
                    }
                }
            });
        }

        if (btnSendQuickMessage != null) {
            btnSendQuickMessage.setOnClickListener(v -> sendQuickMessage());
        }

        if (etQuickMessage != null) {
            etQuickMessage.setOnEditorActionListener((v, actionId, event) -> {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    sendQuickMessage();
                    return true;
                }
                return false;
            });
        }

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) return true;

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
            selectedFriendId = null;
            for (Map.Entry<String, Marker> entry : friendMarkers.entrySet()) {
                if (entry.getValue().getId().equals(marker.getId())) {
                    selectedFriendId = entry.getKey();
                    break;
                }
            }
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (fabDirections != null) fabDirections.setVisibility(View.VISIBLE);
            if (layoutMarkerEmojiTease != null && selectedFriendId != null) {
                layoutMarkerEmojiTease.setVisibility(View.VISIBLE);
            }
            marker.showInfoWindow();
            return true;
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
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
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

        LocationRequest locationRequest = new LocationRequest.Builder(
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
                        showMyAvatarOnMap(myLatLng);
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
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi đẩy định vị: " + e.getMessage()));
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

    // ✅ Hiệu ứng vòng tròn tỏa ra cho người online
    private void startPulseEffect(String friendId, LatLng latLng) {
        // Dừng hiệu ứng cũ nếu có
        stopPulseEffect(friendId);

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(30)
                .strokeColor(Color.argb(200, 30, 136, 229))
                .fillColor(Color.argb(40, 30, 136, 229))
                .strokeWidth(3f));

        friendCircles.put(friendId, circle);

        Handler handler = new Handler(Looper.getMainLooper());
        final float[] radius = {30f};
        final int[] alpha = {200};
        final boolean[] expanding = {true};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!friendCircles.containsKey(friendId)) return;

                Circle c = friendCircles.get(friendId);
                if (c == null) return;

                if (expanding[0]) {
                    radius[0] += 5f;
                    alpha[0] -= 8;
                    if (radius[0] >= 120f) {
                        expanding[0] = false;
                    }
                } else {
                    radius[0] = 30f;
                    alpha[0] = 200;
                    expanding[0] = true;
                }

                c.setRadius(radius[0]);
                c.setCenter(latLng);
                c.setStrokeColor(Color.argb(Math.max(alpha[0], 0), 30, 136, 229));
                c.setFillColor(Color.argb(Math.max(alpha[0] / 5, 0), 30, 136, 229));

                handler.postDelayed(this, 50);
            }
        };

        pulseHandlers.put(friendId, handler);
        pulseRunnables.put(friendId, runnable);
        handler.post(runnable);
    }

    private void stopPulseEffect(String friendId) {
        Handler oldHandler = pulseHandlers.get(friendId);
        Runnable oldRunnable = pulseRunnables.get(friendId);
        if (oldHandler != null && oldRunnable != null) {
            oldHandler.removeCallbacks(oldRunnable);
        }
        pulseHandlers.remove(friendId);
        pulseRunnables.remove(friendId);

        Circle oldCircle = friendCircles.get(friendId);
        if (oldCircle != null) {
            oldCircle.remove();
        }
        friendCircles.remove(friendId);
    }

    private void stopAllPulseEffects() {
        for (String friendId : new ArrayList<>(pulseHandlers.keySet())) {
            stopPulseEffect(friendId);
        }
    }

    private void clearRealtimeListenersAnToan() {
        int count = Math.min(realtimeListeners.size(), listenedRefs.size());
        for (int i = count - 1; i >= 0; i--) {
            try {
                if (listenedRefs.get(i) != null && realtimeListeners.get(i) != null) {
                    listenedRefs.get(i).removeEventListener(realtimeListeners.get(i));
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi giải phóng listener: " + i);
            }
        }
        realtimeListeners.clear();
        listenedRefs.clear();
    }

    private void loadFriendsAndShowOnMap() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUid = mAuth.getCurrentUser().getUid();

        firestoreListener = firestore.collection("friendRequests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

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

                    for (String friendId : friendIds) {
                        DatabaseReference friendLocationRef = locationRef.child(friendId);

                        ValueEventListener vListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) return;

                                Object latObj = snapshot.child("latitude").getValue();
                                Object lngObj = snapshot.child("longitude").getValue();
                                Object isOnlineObj = snapshot.child("isOnline").getValue();
                                Object timestampObj = snapshot.child("timestamp").getValue();

                                if (latObj == null || lngObj == null) return;

                                double lat = Double.parseDouble(latObj.toString());
                                double lng = Double.parseDouble(lngObj.toString());
                                LatLng friendLatLng = new LatLng(lat, lng);

                                boolean isOnline = false;
                                if (isOnlineObj != null && Boolean.parseBoolean(isOnlineObj.toString())) {
                                    if (timestampObj != null) {
                                        long lastUpdate = Long.parseLong(timestampObj.toString());
                                        isOnline = (System.currentTimeMillis() - lastUpdate) < 60000;
                                    }
                                }
                                friendOnlineStatus.put(friendId, isOnline);
                                final boolean finalIsOnline = isOnline;

                                String name = friendNamesCache.get(friendId);
                                if (name != null) {
                                    updateMarkerOnMap(friendId, friendLatLng, name, finalIsOnline);
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
                                                updateMarkerOnMap(friendId, friendLatLng, finalName, finalIsOnline);
                                            })
                                            .addOnFailureListener(e ->
                                                    updateMarkerOnMap(friendId, friendLatLng, "Bạn bè", finalIsOnline));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Lỗi Realtime DB: " + error.getMessage());
                            }
                        };

                        friendLocationRef.addValueEventListener(vListener);
                        listenedRefs.add(friendLocationRef);
                        realtimeListeners.add(vListener);
                    }
                });
    }

    private void showMyAvatarOnMap(LatLng myLatLng) {
        if (mAuth.getCurrentUser() == null || mMap == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        firestore.collection("users").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name = doc.getString("hoTen");
                    if (name == null) name = doc.getString("displayName");
                    if (name == null) name = "Tôi";
                    final String finalName = name;

                    String avatarBase64 = doc.getString("avatarBase64");

                    Marker myMarker = mMap.addMarker(new MarkerOptions()
                            .position(myLatLng)
                            .title("Bạn: " + finalName));

                    if (myMarker != null) {
                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                            try {
                                byte[] bytes = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT);
                                Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (bmp != null) {
                                    Bitmap circular = makeCircularBitmap(bmp, 120);
                                    myMarker.setIcon(BitmapDescriptorFactory.fromBitmap(circular));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi load avatar bản thân: " + e.getMessage());
                            }
                        }
                    }
                });
    }

    private void updateMarkerOnMap(String friendId, LatLng latLng, String title, boolean isOnline) {
        if (mMap == null) return;

        runOnUiThread(() -> {
            if (friendMarkers.containsKey(friendId)) {
                Marker marker = friendMarkers.get(friendId);
                if (marker != null) {
                    marker.setPosition(latLng);
                    marker.setTitle(title);
                }
                Circle circle = friendCircles.get(friendId);
                if (circle != null) circle.setCenter(latLng);
                if (pulseHandlers.containsKey(friendId)) {
                    stopPulseEffect(friendId);
                    Boolean online = friendOnlineStatus.get(friendId);
                    if (online != null && online) {
                        startPulseEffect(friendId, latLng);
                    }
                }
            } else {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(title));
                if (marker != null) {
                    friendMarkers.put(friendId, marker);

                    // Kiểm tra cache avatar trước
                    String cachedAvatar = friendAvatarCache.get(friendId);
                    if (cachedAvatar != null && !cachedAvatar.isEmpty()) {
                        setMarkerFromBase64(friendId, cachedAvatar, title);
                    } else {
                        // Lấy avatar từ Firestore
                        firestore.collection("users").document(friendId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String avatarBase64 = userDoc.getString("avatarBase64");
                                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                            friendAvatarCache.put(friendId, avatarBase64);
                                            setMarkerFromBase64(friendId, avatarBase64, title);
                                        } else {
                                            // Không có avatar → dùng ui-avatars
                                            setMarkerFromUrl(friendId, title);
                                        }
                                    } else {
                                        setMarkerFromUrl(friendId, title);
                                    }
                                })
                                .addOnFailureListener(e -> setMarkerFromUrl(friendId, title));
                    }
                }
            }

            if (isOnline) {
                if (!pulseHandlers.containsKey(friendId)) {
                    startPulseEffect(friendId, latLng);
                }
            } else {
                stopPulseEffect(friendId);
            }
        });
    }

    private void setMarkerFromBase64(String friendId, String base64, String title) {
        try {
            byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                Bitmap circular = makeCircularBitmap(bitmap, 120);
                runOnUiThread(() -> {
                    Marker m = friendMarkers.get(friendId);
                    if (m != null) m.setIcon(BitmapDescriptorFactory.fromBitmap(circular));
                });
            }
        } catch (Exception e) {
            setMarkerFromUrl(friendId, title);
        }
    }

    private void setMarkerFromUrl(String friendId, String title) {
        String avatarUrl = "https://ui-avatars.com/api/?name=" + Uri.encode(title)
                + "&background=random&size=128&rounded=true";
        Glide.with(getApplicationContext())
                .asBitmap()
                .load(avatarUrl)
                .override(120, 120)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                        Marker m = friendMarkers.get(friendId);
                        if (m != null) m.setIcon(BitmapDescriptorFactory.fromBitmap(resource));
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    private Bitmap makeCircularBitmap(Bitmap bitmap, int size) {
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, size, size, true);
        canvas.drawBitmap(scaled, 0, 0, paint);
        return output;
    }

    private void setupTeaseEmojiClickListeners() {
        int[] emojiButtonIds = {
                R.id.btnTeaseFire, R.id.btnTeaseLaugh,
                R.id.btnTeaseGhost, R.id.btnTeasePoop,
                R.id.btnTeaseHeart, R.id.btnTeaseWave
        };
        String[] emojis = {"🔥", "😂", "👻", "💩", "❤️", "👋"};

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
                                Toast.makeText(MainActivity.this,
                                        "Đã thả " + emoji + " trêu bạn!", Toast.LENGTH_SHORT).show();
                                if (layoutMarkerEmojiTease != null) {
                                    layoutMarkerEmojiTease.setVisibility(View.GONE);
                                }
                            });
                });
            }
        }
    }

    private void sendQuickMessage() {
        if (selectedFriendId == null || mAuth.getCurrentUser() == null) return;
        if (etQuickMessage == null) return;

        String text = etQuickMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String myUid = mAuth.getCurrentUser().getUid();

        // Tạo chatRoomId giống ChatActivity
        List<String> ids = new ArrayList<>();
        ids.add(myUid);
        ids.add(selectedFriendId);
        java.util.Collections.sort(ids);
        String chatRoomId = ids.get(0) + "_" + ids.get(1);

        Map<String, Object> message = new HashMap<>();
        message.put("chatRoomId", chatRoomId);
        message.put("senderId", myUid);
        message.put("text", text);
        message.put("emojiTease", "");
        message.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection("messages")
                .add(message)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Đã gửi tin nhắn!", Toast.LENGTH_SHORT).show();
                    etQuickMessage.setText("");
                    if (layoutMarkerEmojiTease != null) {
                        layoutMarkerEmojiTease.setVisibility(View.GONE);
                    }
                    if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi gửi tin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

                    Toast.makeText(MainActivity.this,
                            "⚡ " + senderName + " vừa thả " + emoji + " trêu chọc bạn!",
                            Toast.LENGTH_LONG).show();

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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Cần quyền vị trí để hiển thị bản đồ!", Toast.LENGTH_SHORT).show();
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
        stopAllPulseEffects();
        clearRealtimeListenersAnToan();
        super.onDestroy();
    }
}