package com.example.clubhome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.ui.auth.BaseballNavy

@Composable
fun AppDrawerContent(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToPlayers: () -> Unit,
    onNavigateToTeams: () -> Unit,
    onSignOut: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = BaseballNavy,
        drawerContentColor = Color.White,
        modifier = Modifier.width(260.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(BaseballNavy)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header con icono de menú y usuario
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCloseDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Cerrar Menú",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Perfil",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Opciones de navegación
                DrawerNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentRoute == "home",
                    onClick = {
                        onCloseDrawer()
                        onNavigateToHome()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DrawerNavItem(
                    label = "Jugadores",
                    icon = Icons.Default.People,
                    isSelected = currentRoute == "players",
                    onClick = {
                        onCloseDrawer()
                        onNavigateToPlayers()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DrawerNavItem(
                    label = "Equipos",
                    icon = Icons.Default.Group,
                    isSelected = currentRoute == "teams",
                    onClick = {
                        onCloseDrawer()
                        onNavigateToTeams()
                    }
                )
            }

            // Botón Cerrar Sesión abajo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCloseDrawer()
                        onSignOut()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "cerrar sesion",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = Color.White
        )
    }
}
