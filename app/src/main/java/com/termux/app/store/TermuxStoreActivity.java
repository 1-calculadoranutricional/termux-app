package com.termux.app.store;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.net.uri.UriUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TermuxStoreActivity extends AppCompatActivity implements StorePackageAdapter.OnPackageActionListener {

    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private StorePackageAdapter mAdapter;
    private List<StorePackageItem> mAllPackages;
    private List<StorePackageItem> mShuffledForYouPackages;
    private EditText mSearchInput;
    private ImageView mClearButton;
    private String mCurrentCategoryFilter = null;

    private TextView mChipForYou;
    private TextView mChipGoogle;
    private TextView mChipTopTools;
    private TextView mChipDev;
    private TextView mChipSystem;
    private TextView mChipAiCli;

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, TermuxStoreActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure status bar & navigation bar are dark Play Store background with no red margin
        getWindow().setStatusBarColor(Color.parseColor("#000000"));
        getWindow().setNavigationBarColor(Color.parseColor("#000000"));

        setContentView(R.layout.activity_store);

        mRecyclerView = findViewById(R.id.store_packages_recycler_view);
        mEmptyView = findViewById(R.id.store_empty_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        initPackageList();
        mShuffledForYouPackages = new ArrayList<>(mAllPackages);
        Collections.shuffle(mShuffledForYouPackages);
        mAdapter = new StorePackageAdapter(new ArrayList<>(mShuffledForYouPackages), this);
        mRecyclerView.setAdapter(mAdapter);

        // Back button inside the Play Store search bar
        View backButton = findViewById(R.id.store_back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        // Search input and clear button
        mSearchInput = findViewById(R.id.store_search_input);
        mClearButton = findViewById(R.id.store_search_clear);

        if (mClearButton != null) {
            mClearButton.setOnClickListener(v -> {
                if (mSearchInput != null) {
                    mSearchInput.setText("");
                }
            });
        }

        if (mSearchInput != null) {
            mSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    v.clearFocus();
                    return true;
                }
                return false;
            });

            mSearchInput.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    v.clearFocus();
                    return true;
                }
                return false;
            });

            mSearchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (mClearButton != null) {
                        mClearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                    filterPackages(s.toString(), mCurrentCategoryFilter);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Category chips
        mChipForYou = findViewById(R.id.chip_for_you);
        mChipGoogle = findViewById(R.id.chip_google);
        mChipTopTools = findViewById(R.id.chip_top_tools);
        mChipDev = findViewById(R.id.chip_dev);
        mChipSystem = findViewById(R.id.chip_system);
        mChipAiCli = findViewById(R.id.chip_ai_cli);

        setupChips();
        updateChipVisuals();
    }

    private void setupChips() {
        if (mChipForYou != null) {
            mChipForYou.setOnClickListener(v -> selectCategoryChip(null));
        }
        if (mChipSystem != null) {
            mChipSystem.setOnClickListener(v -> selectCategoryChip("sistema"));
        }
        if (mChipTopTools != null) {
            mChipTopTools.setOnClickListener(v -> selectCategoryChip("herramientas"));
        }
        if (mChipDev != null) {
            mChipDev.setOnClickListener(v -> selectCategoryChip("desarrollo"));
        }
        if (mChipGoogle != null) {
            mChipGoogle.setOnClickListener(v -> selectCategoryChip("google"));
        }
        if (mChipAiCli != null) {
            mChipAiCli.setOnClickListener(v -> selectCategoryChip("ia"));
        }
    }

    private void selectCategoryChip(@Nullable String category) {
        mCurrentCategoryFilter = category;
        if (category == null) {
            mShuffledForYouPackages = new ArrayList<>(mAllPackages);
            Collections.shuffle(mShuffledForYouPackages);
        }
        updateChipVisuals();
        filterPackages(mSearchInput != null ? mSearchInput.getText().toString() : "", mCurrentCategoryFilter);
    }

    private void updateChipVisuals() {
        int selectedBg = R.drawable.bg_store_chip_selected;
        int unselectedBg = R.drawable.bg_store_chip_unselected;
        int selectedText = Color.parseColor("#000000");
        int unselectedText = Color.parseColor("#9AA0A6");

        setChipState(mChipForYou, mCurrentCategoryFilter == null, selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipSystem, "sistema".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipTopTools, "herramientas".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipDev, "desarrollo".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipGoogle, "google".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipAiCli, "ia".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
    }

    private void setChipState(TextView chip, boolean isSelected, int selectedBg, int unselectedBg, int selectedText, int unselectedText) {
        if (chip == null) return;
        chip.setBackgroundResource(isSelected ? selectedBg : unselectedBg);
        chip.setTextColor(isSelected ? selectedText : unselectedText);
    }

    private void filterPackages(String query, String categoryFilter) {
        List<StorePackageItem> filtered = new ArrayList<>();
        String normalizedQuery = query != null ? query.trim().toLowerCase() : "";

        List<StorePackageItem> sourceList = (categoryFilter == null || categoryFilter.trim().isEmpty())
            ? mShuffledForYouPackages
            : mAllPackages;

        for (StorePackageItem item : sourceList) {
            boolean matchesQuery = normalizedQuery.isEmpty() ||
                item.getTitle().toLowerCase().contains(normalizedQuery) ||
                item.getDescription().toLowerCase().contains(normalizedQuery) ||
                item.getCategory().toLowerCase().contains(normalizedQuery);

            boolean matchesCategory = categoryFilter == null || categoryFilter.trim().isEmpty() ||
                categoryFilter.equalsIgnoreCase(item.getCategoryId());

            if (matchesQuery && matchesCategory) {
                filtered.add(item);
            }
        }

        if (mAdapter != null) {
            mAdapter.updateList(filtered);
        }

        if (filtered.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            if (mEmptyView != null) mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            if (mEmptyView != null) mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initPackageList() {
        mAllPackages = new ArrayList<>();
        initSystemPackages();
        initToolsPackages();
        initDevPackages();
        initGooglePackages();
        initAiCliPackages();
    }

    private void initAiCliPackages() {
        // ==========================================
        // CATEGORÍA AI & CLI ("ia")
        // Exact order: OpenWiki, CodeGraph, Engram
        // ==========================================

        // OpenWiki CLI
        mAllPackages.add(new StorePackageItem(
            "openwiki",
            "OpenWiki CLI",
            "ia",
            "IA y CLI • Wiki & Arquitectura",
            "Herramienta CLI para generar y consultar documentación de arquitectura, grafos de dependencias y wikis técnicas.",
            R.drawable.ic_pkg_openwiki,
            "npm install -g --allow-scripts=node-pty,@github/keytar,better-sqlite3 openwiki",
            "npm uninstall -g openwiki",
            "npm install -g --allow-scripts=node-pty,@github/keytar,better-sqlite3 openwiki",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/openwiki:" + TermuxConstants.TERMUX_HOME_DIR_PATH + "/.local/bin/openwiki",
            "4.9 ★",
            "18 MB",
            "openwiki",
            "openwiki --help",
            null
        ));

        // 1.2 CodeGraph CLI
        mAllPackages.add(new StorePackageItem(
            "codegraph",
            "CodeGraph CLI",
            "ia",
            "IA y CLI • Navegación & Grafos",
            "Instala y configura CodeGraph CLI para análisis de código y navegación de bases de código con grafos.",
            R.drawable.ic_pkg_code,
            "curl -fsSL https://raw.githubusercontent.com/colbymchenry/codegraph/main/install.sh | sh\n" +
            "echo 'export PATH=\"$HOME/.local/bin:$PATH\"' >> ~/.zshrc\n" +
            "export PATH=\"$HOME/.local/bin:$PATH\"\n" +
            "mkdir -p \"$HOME/.local/bin\"\n" +
            "cat << 'EOF' > \"$HOME/.local/bin/codegraph\"\n" +
            "#!/data/data/com.termux/files/usr/bin/sh\n" +
            "exec node \\\n" +
            "  --disable-warning=ExperimentalWarning \\\n" +
            "  \"$HOME/.codegraph/versions/v1.6.2/lib/dist/bin/codegraph.js\" \"$@\"\n" +
            "EOF\n" +
            "chmod +x \"$HOME/.local/bin/codegraph\"",
            "rm -rf ~/.codegraph ~/.local/bin/codegraph",
            "curl -fsSL https://raw.githubusercontent.com/colbymchenry/codegraph/main/install.sh | sh",
            "/data/data/com.termux/files/home/.local/bin/codegraph",
            "4.9 ★",
            "10 MB",
            "codegraph",
            "codegraph --help",
            null
        ));

        // 1.3 Engram
        mAllPackages.add(new StorePackageItem(
            "engram",
            "Engram",
            "ia",
            "IA y CLI • Memoria Persistente",
            "Capa de memoria persistente para agentes y herramientas de IA (Antigravity CLI, Gemini CLI).",
            R.drawable.ic_pkg_engram,
            "command -v go >/dev/null 2>&1 || pkg install -y golang\n" +
            "go install github.com/Gentleman-Programming/engram/v3/cmd/engram@latest\n" +
            "grep -q 'GOPATH' ~/.zshrc 2>/dev/null || echo 'export GOPATH=$HOME/go' >> ~/.zshrc\n" +
            "grep -q 'GOPATH/bin' ~/.zshrc 2>/dev/null || echo 'export PATH=$PATH:$GOPATH/bin' >> ~/.zshrc\n" +
            "export GOPATH=$HOME/go\n" +
            "export PATH=$PATH:$GOPATH/bin\n" +
            "[ -f ~/.zshrc ] && source ~/.zshrc 2>/dev/null || true",
            "rm -f ~/go/bin/engram",
            "go install github.com/Gentleman-Programming/engram/v3/cmd/engram@latest",
            TermuxConstants.TERMUX_HOME_DIR_PATH + "/go/bin/engram:" + TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/engram",
            "4.9 ★",
            "15 MB",
            "engram",
            "engram --help",
            null
        ));
    }

    private void initSystemPackages() {
        // ==========================================
        // CATEGORÍA SYSTEM ("sistema")
        // Exact order:
        // 1. Hushlogin (Silenciar Bienvenida)
        // 2. Cambiar repositorio
        // 3. Actualizar sistema
        // 4. Termux storage
        // 5. Termux backup
        // 6. Termux restore
        // 7. Test velocidad mirror
        // ==========================================

        // 2.1 Hushlogin (Silenciar Bienvenida)
        mAllPackages.add(new StorePackageItem(
            "hushlogin",
            "Hushlogin",
            "sistema",
            "Sistema • Silenciar Bienvenida",
            "Oculta el mensaje de bienvenida y motd al abrir una nueva terminal en Termux (touch ~/.hushlogin).",
            R.drawable.ic_pkg_terminal,
            "touch ~/.hushlogin",
            "rm -f ~/.hushlogin",
            "touch ~/.hushlogin",
            TermuxConstants.TERMUX_HOME_DIR_PATH + "/.hushlogin",
            "4.9 ★",
            "1 KB",
            null,
            "rm -f ~/.hushlogin",
            null
        ));

        // 2.2 Cambiar Repositorio
        mAllPackages.add(new StorePackageItem(
            "termux-change-repo",
            "Cambiar Repositorio",
            "sistema",
            "Sistema • Repositorios",
            "Ejecuta termux-change-repo para seleccionar espejos (mirrors) de APT rápidos y recomendados.",
            R.drawable.ic_pkg_terminal,
            "termux-change-repo",
            null,
            null,
            null,
            "4.9 ★",
            "1 MB",
            "termux-change-repo",
            "pkg help",
            null
        ));

        // 2.2 Actualizar Sistema
        mAllPackages.add(new StorePackageItem(
            "pkg-upgrade-auto",
            "Actualizar Sistema",
            "sistema",
            "Sistema • Actualización",
            "Actualiza todos los paquetes e instala actualizaciones manteniendo las configuraciones locales de forma automática.",
            R.drawable.ic_pkg_monitor,
            "pkg update -y && pkg upgrade -y -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\"",
            null,
            null,
            null,
            "4.9 ★",
            "15 MB",
            null,
            "pkg help",
            null
        ));

        // 2.3 Termux Storage
        mAllPackages.add(new StorePackageItem(
            "termux-setup-storage",
            "Termux Storage",
            "sistema",
            "Sistema • Almacenamiento Compartido",
            "Configura el acceso al almacenamiento compartido de Android (~/storage/shared, descargas y fotos).",
            R.drawable.ic_pkg_storage,
            "termux-setup-storage",
            null,
            null,
            null,
            "4.9 ★",
            "1 MB",
            "cd ~/storage/shared",
            "ls ~/storage",
            null
        ));

        // 2.4 Termux Backup
        mAllPackages.add(new StorePackageItem(
            "termux-backup",
            "Termux Backup",
            "sistema",
            "Sistema • Copia de Seguridad",
            "Crea una copia de seguridad comprimida completa de home y usr en /sdcard/Download/termux/termux-backup.tar.gz.",
            R.drawable.ic_pkg_backup,
            "mkdir -p /sdcard/Download/termux && cd /data/data/com.termux/files && tar -zcvf /sdcard/Download/termux/termux-backup.tar.gz home usr",
            null,
            null,
            null,
            "4.9 ★",
            "2 MB",
            "ls -lh /sdcard/Download/termux/termux-backup.tar.gz",
            "tar --help",
            null
        ));

        // 2.5 Termux Restore
        mAllPackages.add(new StorePackageItem(
            "termux-restore",
            "Termux Restore",
            "sistema",
            "Sistema • Restaurar Copia",
            "Restaura la copia de seguridad previa desde /sdcard/Download/termux/termux-backup.tar.gz.",
            R.drawable.ic_pkg_backup,
            "cd /data/data/com.termux/files && tar -zxvf /sdcard/Download/termux/termux-backup.tar.gz --recursive-unlink",
            null,
            null,
            null,
            "4.8 ★",
            "2 MB",
            "exit",
            "tar --help",
            null
        ));

        // 2.6 Test Velocidad Mirrors
        mAllPackages.add(new StorePackageItem(
            "mirror-speed-test",
            "Test Velocidad Mirrors",
            "sistema",
            "Sistema • Test Velocidad Mirrors",
            "Prueba la velocidad de descarga y latencia de los espejos de Termux a nivel mundial (Purdue, FAU, SUSTech, Middlendian, Repository.su).",
            R.drawable.ic_pkg_monitor,
            "for servidor in \\\n" +
            "  \"Norteamérica (Purdue):https://plug-mirror.rcac.purdue.edu/termux/termux-main/dists/stable/main/binary-aarch64/Packages\" \\\n" +
            "  \"Europa (Alemania - FAU):https://ftp.fau.de/termux/termux-main/dists/stable/main/binary-aarch64/Packages\" \\\n" +
            "  \"China (SUSTech):https://mirrors.sustech.edu.cn/termux/apt/termux-main/dists/stable/main/binary-aarch64/Packages\" \\\n" +
            "  \"Oceanía (Middlendian):https://mirrors.middlendian.com/termux/termux-main/dists/stable/main/binary-aarch64/Packages\" \\\n" +
            "  \"Rusia (Repository.su):https://repository.su/termux/termux-main/dists/stable/main/binary-aarch64/Packages\"; do\n" +
            "    nombre=\"${servidor%%:*}\"\n" +
            "    url=\"${servidor#*:}\"\n" +
            "    echo \"----------------------------------------\"\n" +
            "    echo \"Probando servidor: $nombre\"\n" +
            "    curl -s -o /dev/null -w \"-> Velocidad: %{speed_download} bytes/sec | Tiempo: %{time_total} seg\\n\" \"$url\"\n" +
            "done\n" +
            "echo \"----------------------------------------\"\n" +
            "echo \"¡Pruebas finalizadas!\"",
            null,
            null,
            null,
            "4.8 ★",
            "2 MB",
            "termux-change-repo",
            "pkg help",
            null
        ));
    }

    private void initDevPackages() {
        // ==========================================
        // CATEGORÍA DEV ("desarrollo")
        // Exact order: npm, python, node, git, git config, gh cli
        // ==========================================

        // 3.1 NPM
        mAllPackages.add(new StorePackageItem(
            "npm-update",
            "NPM",
            "desarrollo",
            "Desarrollo • Node.js Package Manager",
            "Instala o actualiza Node.js/NPM y ejecuta npm install -g npm para tener la última versión global.",
            R.drawable.ic_pkg_code,
            "pkg install npm -y && npm install -g npm",
            "pkg uninstall npm -y",
            "pkg install npm -y && npm install -g npm",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/npm",
            "4.8 ★",
            "18 MB",
            "npm",
            "npm --help",
            null
        ));

        // 3.2 Python
        mAllPackages.add(new StorePackageItem(
            "python",
            "Python",
            "desarrollo",
            "Desarrollo • Lenguaje de Programación",
            "Lenguaje de programación interpretado de alto nivel y propósitos generales con el gestor pip.",
            R.drawable.ic_pkg_code,
            "pkg install python -y",
            "pkg uninstall python -y",
            "pkg install python -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/python:" + TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/python3",
            "4.9 ★",
            "45 MB",
            "python",
            "python --help",
            null
        ));

        // 3.3 Node.js LTS
        mAllPackages.add(new StorePackageItem(
            "nodejs-lts",
            "Node.js LTS",
            "desarrollo",
            "Desarrollo • Javascript Runtime",
            "Entorno de ejecución para JavaScript en el servidor (Long Term Support) con gestor de paquetes npm.",
            R.drawable.ic_pkg_code,
            "pkg install nodejs-lts -y",
            "pkg uninstall nodejs-lts -y",
            "pkg install nodejs-lts -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/node",
            "4.8 ★",
            "35 MB",
            "node",
            "node --help",
            null
        ));

        // 3.4 Git
        mAllPackages.add(new StorePackageItem(
            "git",
            "Git",
            "desarrollo",
            "Desarrollo • Control de Versiones",
            "Sistema de control de versiones distribuido rápido y escalable para repositorios y código fuente.",
            R.drawable.ic_pkg_git,
            "pkg install git -y",
            "pkg uninstall git -y",
            "pkg install git -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/git",
            "4.9 ★",
            "18 MB",
            "git",
            "git --help",
            null
        ));

        // 3.5 Git Config
        mAllPackages.add(new StorePackageItem(
            "git-config",
            "Git Config",
            "desarrollo",
            "Desarrollo • Configurar Usuario",
            "Configura de forma interactiva tu nombre de usuario y correo global en Git.",
            R.drawable.ic_pkg_git,
            "python3 -c '\n" +
            "import subprocess\n" +
            "u = input(\"Usuario (user.name): \")\n" +
            "e = input(\"Correo (user.email): \")\n" +
            "subprocess.run([\"git\", \"config\", \"--global\", \"user.name\", u])\n" +
            "subprocess.run([\"git\", \"config\", \"--global\", \"user.email\", e])\n" +
            "print(\"¡Listo! Configurado con éxito.\")\n" +
            "'",
            null,
            null,
            null,
            "4.9 ★",
            "1 KB",
            "git config --list",
            "git config --help",
            null
        ));

        // 3.6 GitHub CLI (gh)
        mAllPackages.add(new StorePackageItem(
            "gh",
            "GitHub CLI (gh)",
            "desarrollo",
            "Desarrollo • Herramientas Git",
            "Herramienta oficial de línea de comandos de GitHub para gestionar pull requests, issues y repositorios.",
            R.drawable.ic_pkg_git,
            "pkg install gh -y",
            "pkg uninstall gh -y",
            "pkg install gh -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/gh",
            "4.9 ★",
            "25 MB",
            "gh auth login",
            "gh --help",
            null
        ));

        // 3.7 Lazygit
        mAllPackages.add(new StorePackageItem(
            "lazygit",
            "Lazygit",
            "desarrollo",
            "Desarrollo • Terminal Git UI",
            "Interfaz de usuario de terminal simple y rápida para comandos Git con navegación visual de ramas y commits.",
            R.drawable.ic_pkg_git,
            "pkg install lazygit -y",
            "pkg uninstall lazygit -y",
            "pkg install lazygit -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/lazygit",
            "4.9 ★",
            "15 MB",
            "lazygit",
            "lazygit --help",
            null
        ));
    }

    private void initToolsPackages() {
        // ==========================================
        // CATEGORÍA TOOLS ("herramientas")
        // Exact order: zsh, yazi, file, iosevka nerd font
        // ==========================================

        // Zsh & Powerlevel10k
        mAllPackages.add(new StorePackageItem(
            "zsh",
            "Zsh & Powerlevel10k",
            "herramientas",
            "Herramientas • Shell Avanzada",
            "Shell Zsh avanzada con Powerlevel10k, autocompletado y resaltado de sintaxis.",
            R.drawable.ic_pkg_terminal,
            "pkg install zsh -y && " +
            "rm -rf ~/.zsh ~/.zshrc && " +
            "mkdir -p ~/.zsh/plugins ~/.zsh/themes && " +
            "git clone --depth=1 https://github.com/romkatv/powerlevel10k.git ~/.zsh/themes/powerlevel10k && " +
            "git clone --depth=1 https://github.com/zsh-users/zsh-syntax-highlighting.git ~/.zsh/plugins/zsh-syntax-highlighting && " +
            "git clone --depth=1 https://github.com/zsh-users/zsh-autosuggestions ~/.zsh/plugins/zsh-autosuggestions && " +
            "cat << 'EOF' > ~/.zshrc\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Powerlevel10k Instant Prompt\n" +
            "# Debe permanecer cerca del inicio para optimizar la carga del shell.\n" +
            "# ------------------------------------------------------------------------------\n" +
            "if [[ -r \"${XDG_CACHE_HOME:-$HOME/.cache}/p10k-instant-prompt-${(%):-%n}.zsh\" ]]; then\n" +
            "  source \"${XDG_CACHE_HOME:-$HOME/.cache}/p10k-instant-prompt-${(%):-%n}.zsh\"\n" +
            "fi\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Theme & Visual Appearance\n" +
            "# ------------------------------------------------------------------------------\n" +
            "source ~/.zsh/themes/powerlevel10k/powerlevel10k.zsh-theme\n" +
            "[[ ! -f ~/.p10k.zsh ]] || source ~/.p10k.zsh\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Plugins\n" +
            "# ------------------------------------------------------------------------------\n" +
            "source ~/.zsh/plugins/zsh-syntax-highlighting/zsh-syntax-highlighting.zsh\n" +
            "source ~/.zsh/plugins/zsh-autosuggestions/zsh-autosuggestions.zsh\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# History Settings\n" +
            "# ------------------------------------------------------------------------------\n" +
            "HISTFILE=~/.zsh_history\n" +
            "HISTSIZE=10000\n" +
            "SAVEHIST=10000\n" +
            "setopt share_history hist_ignore_dups hist_ignore_space hist_verify\n\n" +
            "# Búsqueda en el historial basada en lo ya escrito (Up/Down)\n" +
            "autoload -Uz up-line-or-beginning-search down-line-or-beginning-search\n" +
            "zle -N up-line-or-beginning-search\n" +
            "zle -N down-line-or-beginning-search\n" +
            "bindkey '^[[A' up-line-or-beginning-search    # Flecha Arriba\n" +
            "bindkey '^[OA' up-line-or-beginning-search    # Flecha Arriba (modo aplicación)\n" +
            "bindkey '^[[B' down-line-or-beginning-search  # Flecha Abajo\n" +
            "bindkey '^[OB' down-line-or-beginning-search  # Flecha Abajo (modo aplicación)\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Syntax Highlighting Styles\n" +
            "# ------------------------------------------------------------------------------\n" +
            "typeset -A ZSH_HIGHLIGHT_STYLES\n" +
            "ZSH_HIGHLIGHT_STYLES[command]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[alias]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[builtin]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[function]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[hashed-command]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[precommand]='fg=cyan'\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Aliases\n" +
            "# ------------------------------------------------------------------------------\n" +
            "alias X=\"exit\"\n" +
            "alias C=\"clear\"\n" +
            "alias Cd=\"cd\"\n" +
            "alias Cat=\"cat\"\n" +
            "alias Gemini-proxy=\"node ~/gemini-proxy.mjs\"\n" +
            "alias Agy-proxy=\"node ~/antigravity-proxy.mjs\"\n" +
            "alias Agy=\"agy --model gemini-3.8-flash-low --dangerously-skip-permissions 2>/dev/null\"\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# File Manager Integration (Yazi) & Keybindings\n" +
            "# ------------------------------------------------------------------------------\n" +
            "function yazi() {\n" +
            "  local tmp=\"$(mktemp -t \\\"yazi-cwd.XXXXXX\\\")\" cwd\n" +
            "  command yazi \"$@\" --cwd-file=\"$tmp\"\n" +
            "  IFS= read -r -d '' cwd < \"$tmp\"\n" +
            "  [ \"$cwd\" != \"$PWD\" ] && [ -d \"$cwd\" ] && builtin cd -- \"$cwd\"\n" +
            "  command rm -f -- \"$tmp\"\n" +
            "}\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Environment & PATH\n" +
            "# ------------------------------------------------------------------------------\n" +
            "export PATH=\"$HOME/.local/bin:$PATH\"\n\n" +
            "# Gemini CLI & OpenWiki Gateway (Port 8085)\n" +
            "export GEMINI_KEYS=\"TU_API_KEY_AQUI\"\n" +
            "export GEMINI_API_KEY=\"dummy_key\"\n" +
            "export GOOGLE_GEMINI_BASE_URL=\"http://127.0.0.1:8085\"\n" +
            "export LANGSMITH_GATEWAY=\"http://127.0.0.1:8085\"\n" +
            "export OPENWIKI_PROVIDER=\"gemini\"\n\n" +
            "# Antigravity CLI Proxy (Port 8086)\n" +
            "export CLOUD_CODE_URL=\"http://127.0.0.1:8086\"\n" +
            "export MODEL_GOOGLE_JARVIS_PROXY=\"http://127.0.0.1:8086\"\n" +
            "EOF\n" +
            "chsh -s zsh && exec zsh",
            "pkg uninstall zsh -y",
            "cat << 'EOF' > ~/.zshrc\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Powerlevel10k Instant Prompt\n" +
            "# Debe permanecer cerca del inicio para optimizar la carga del shell.\n" +
            "# ------------------------------------------------------------------------------\n" +
            "if [[ -r \"${XDG_CACHE_HOME:-$HOME/.cache}/p10k-instant-prompt-${(%):-%n}.zsh\" ]]; then\n" +
            "  source \"${XDG_CACHE_HOME:-$HOME/.cache}/p10k-instant-prompt-${(%):-%n}.zsh\"\n" +
            "fi\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Theme & Visual Appearance\n" +
            "# ------------------------------------------------------------------------------\n" +
            "source ~/.zsh/themes/powerlevel10k/powerlevel10k.zsh-theme\n" +
            "[[ ! -f ~/.p10k.zsh ]] || source ~/.p10k.zsh\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Plugins\n" +
            "# ------------------------------------------------------------------------------\n" +
            "source ~/.zsh/plugins/zsh-syntax-highlighting/zsh-syntax-highlighting.zsh\n" +
            "source ~/.zsh/plugins/zsh-autosuggestions/zsh-autosuggestions.zsh\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# History Settings\n" +
            "# ------------------------------------------------------------------------------\n" +
            "HISTFILE=~/.zsh_history\n" +
            "HISTSIZE=10000\n" +
            "SAVEHIST=10000\n" +
            "setopt share_history hist_ignore_dups hist_ignore_space hist_verify\n\n" +
            "# Búsqueda en el historial basada en lo ya escrito (Up/Down)\n" +
            "autoload -Uz up-line-or-beginning-search down-line-or-beginning-search\n" +
            "zle -N up-line-or-beginning-search\n" +
            "zle -N down-line-or-beginning-search\n" +
            "bindkey '^[[A' up-line-or-beginning-search    # Flecha Arriba\n" +
            "bindkey '^[OA' up-line-or-beginning-search    # Flecha Arriba (modo aplicación)\n" +
            "bindkey '^[[B' down-line-or-beginning-search  # Flecha Abajo\n" +
            "bindkey '^[OB' down-line-or-beginning-search  # Flecha Abajo (modo aplicación)\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Syntax Highlighting Styles\n" +
            "# ------------------------------------------------------------------------------\n" +
            "typeset -A ZSH_HIGHLIGHT_STYLES\n" +
            "ZSH_HIGHLIGHT_STYLES[command]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[alias]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[builtin]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[function]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[hashed-command]='fg=cyan'\n" +
            "ZSH_HIGHLIGHT_STYLES[precommand]='fg=cyan'\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Aliases\n" +
            "# ------------------------------------------------------------------------------\n" +
            "alias X=\"exit\"\n" +
            "alias C=\"clear\"\n" +
            "alias Cd=\"cd\"\n" +
            "alias Cat=\"cat\"\n" +
            "alias Gemini-proxy=\"node ~/gemini-proxy.mjs\"\n" +
            "alias Agy-proxy=\"node ~/antigravity-proxy.mjs\"\n" +
            "alias Agy=\"agy --model gemini-3.8-flash-low --dangerously-skip-permissions 2>/dev/null\"\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# File Manager Integration (Yazi) & Keybindings\n" +
            "# ------------------------------------------------------------------------------\n" +
            "function yazi() {\n" +
            "  local tmp=\"$(mktemp -t \\\"yazi-cwd.XXXXXX\\\")\" cwd\n" +
            "  command yazi \"$@\" --cwd-file=\"$tmp\"\n" +
            "  IFS= read -r -d '' cwd < \"$tmp\"\n" +
            "  [ \"$cwd\" != \"$PWD\" ] && [ -d \"$cwd\" ] && builtin cd -- \"$cwd\"\n" +
            "  command rm -f -- \"$tmp\"\n" +
            "}\n\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# Environment & PATH\n" +
            "# ------------------------------------------------------------------------------\n" +
            "export PATH=\"$HOME/.local/bin:$PATH\"\n\n" +
            "# Gemini CLI & OpenWiki Gateway (Port 8085)\n" +
            "export GEMINI_KEYS=\"TU_API_KEY_AQUI\"\n" +
            "export GEMINI_API_KEY=\"dummy_key\"\n" +
            "export GOOGLE_GEMINI_BASE_URL=\"http://127.0.0.1:8085\"\n" +
            "export LANGSMITH_GATEWAY=\"http://127.0.0.1:8085\"\n" +
            "export OPENWIKI_PROVIDER=\"gemini\"\n\n" +
            "# Antigravity CLI Proxy (Port 8086)\n" +
            "export CLOUD_CODE_URL=\"http://127.0.0.1:8086\"\n" +
            "export MODEL_GOOGLE_JARVIS_PROXY=\"http://127.0.0.1:8086\"\n" +
            "EOF\n" +
            "chsh -s zsh && exec zsh",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/zsh",
            "4.9 ★",
            "20 MB",
            "exec zsh",
            "p10k configure",
            null
        ));

        // 4.2 Yazi File Manager
        mAllPackages.add(new StorePackageItem(
            "yazi",
            "Yazi",
            "herramientas",
            "Herramientas • Terminal File Manager",
            "Navegador de archivos ultrarrápido escrito en Rust con soporte para vista previa gráfica y asíncrona.",
            R.drawable.ic_pkg_yazi,
            "pkg install yazi -y && \\\n" +
            "mkdir -p ~/.config/yazi && \\\n" +
            "cat << 'EOF' > ~/.config/yazi/keymap.toml\n" +
            "[[mgr.prepend_keymap]]\n" +
            "on   = \"<Esc>\"\n" +
            "run  = \"close\"\n" +
            "desc = \"Cancel input\"\n" +
            "EOF\n" +
            "grep -q \"File Manager Integration (Yazi)\" ~/.zshrc 2>/dev/null || cat << 'EOF' >> ~/.zshrc\n" +
            "\n" +
            "# ------------------------------------------------------------------------------\n" +
            "# File Manager Integration (Yazi) & Keybindings\n" +
            "# ------------------------------------------------------------------------------\n" +
            "function yazi() {\n" +
            "  local tmp=\"$(mktemp -t \"yazi-cwd.XXXXXX\")\" cwd\n" +
            "  command yazi \"$@\" --cwd-file=\"$tmp\"\n" +
            "  IFS= read -r -d '' cwd < \"$tmp\"\n" +
            "  [ \"$cwd\" != \"$PWD\" ] && [ -d \"$cwd\" ] && builtin cd -- \"$cwd\"\n" +
            "  command rm -f -- \"$tmp\"\n" +
            "}\n" +
            "EOF",
            "pkg uninstall yazi -y",
            "pkg install yazi -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/yazi",
            "4.9 ★",
            "14 MB",
            "yazi",
            "yazi --help",
            null
        ));

        // 4.3 File
        mAllPackages.add(new StorePackageItem(
            "file",
            "File",
            "herramientas",
            "Herramientas • Utilidad de Archivos",
            "Determina el tipo de un archivo mediante análisis de cabeceras y pruebas mágicas.",
            R.drawable.ic_pkg_code,
            "pkg install file -y",
            "pkg uninstall file -y",
            "pkg install file -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/file",
            "4.8 ★",
            "3 MB",
            "file <archivo>",
            "file --help",
            null
        ));

        // 4.4 Iosevka Nerd Font
        mAllPackages.add(new StorePackageItem(
            "iosevka-nerd-font",
            "Iosevka Nerd Font",
            "herramientas",
            "Herramientas • Tipografía & Iconos",
            "Fuente monoespaciada con soporte para glifos Nerd Fonts y ligaduras para la terminal.",
            R.drawable.ic_pkg_font,
            "mkdir -p ~/.termux && \\\n" +
            "curl -L -o ~/.termux/Iosevka.zip https://github.com/ryanoasis/nerd-fonts/releases/latest/download/Iosevka.zip && \\\n" +
            "unzip -o ~/.termux/Iosevka.zip -d ~/.termux/ && \\\n" +
            "cp ~/.termux/IosevkaNerdFont-Regular.ttf ~/.termux/font.ttf && \\\n" +
            "rm ~/.termux/Iosevka.zip && \\\n" +
            "termux-reload-settings",
            "rm -f ~/.termux/font.ttf && termux-reload-settings",
            "mkdir -p ~/.termux && curl -L -o ~/.termux/Iosevka.zip https://github.com/ryanoasis/nerd-fonts/releases/latest/download/Iosevka.zip && unzip -o ~/.termux/Iosevka.zip -d ~/.termux/ && cp ~/.termux/IosevkaNerdFont-Regular.ttf ~/.termux/font.ttf && rm ~/.termux/Iosevka.zip && termux-reload-settings",
            TermuxConstants.TERMUX_HOME_DIR_PATH + "/.termux/font.ttf",
            "4.9 ★",
            "8 MB",
            "termux-reload-settings",
            "rm -f ~/.termux/font.ttf && termux-reload-settings",
            null
        ));

        // 4.5 Which
        mAllPackages.add(new StorePackageItem(
            "which",
            "Which",
            "herramientas",
            "Herramientas • Localizador de Binarios",
            "Localiza y muestra la ruta completa de ejecutables en el entorno PATH.",
            R.drawable.ic_pkg_terminal,
            "pkg install which -y",
            "pkg uninstall which -y",
            "pkg install which -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/which",
            "4.8 ★",
            "1 MB",
            "which <comando>",
            "which --help",
            null
        ));
    }

    private void initGooglePackages() {
        // ==========================================
        // CATEGORÍA GOOGLE ("google")
        // Exact order: Antigravity CLI, Gemini CLI, Angular CLI, Golang
        // ==========================================

        // Antigravity CLI
        mAllPackages.add(new StorePackageItem(
            "antigravity-cli",
            "Antigravity CLI",
            "google",
            "Google • Agente IA Avanzado",
            "Agente inteligente oficial de Google para desarrollo de software avanzado, pairing y automatización completa.",
            R.drawable.ic_pkg_antigravity,
            "curl -fsSL https://antigravity.google/cli/install.sh | bash && " +
            "echo 'export PATH=\"/data/data/com.termux/files/home/.local/bin:$PATH\"' >> ~/.zshrc && source ~/.zshrc",
            "rm -rf ~/.gemini/antigravity-cli ~/.local/bin/agy",
            "curl -fsSL https://antigravity.google/cli/install.sh | bash",
            TermuxConstants.TERMUX_HOME_DIR_PATH + "/.local/bin/agy:" + TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/agy",
            "4.9 ★",
            "22 MB",
            "agy",
            "agy --help",
            null
        ));

        // 5.2 Gemini CLI
        mAllPackages.add(new StorePackageItem(
            "gemini-cli",
            "Gemini CLI",
            "google",
            "Google • Asistente de IA",
            "Línea de comandos oficial de Google Gemini para interactuar con modelos de IA, generación de código y automatización multimodal.",
            R.drawable.ic_pkg_gemini,
            "(command -v npm >/dev/null 2>&1 || pkg install -y nodejs-lts) && " +
            "npm install -g --allow-scripts=node-pty,@github/keytar @google/gemini-cli",
            "npm uninstall -g @google/gemini-cli",
            "npm install -g --allow-scripts=node-pty,@github/keytar @google/gemini-cli",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/gemini:" + TermuxConstants.TERMUX_HOME_DIR_PATH + "/.local/bin/gemini",
            "4.9 ★",
            "24 MB",
            "gemini",
            "gemini --help",
            null
        ));

        // 5.3 Angular CLI
        mAllPackages.add(new StorePackageItem(
            "angular-cli",
            "Angular CLI",
            "google",
            "Google • Framework Web",
            "Herramienta oficial de línea de comandos de Google para inicializar, compilar y mantener aplicaciones Angular.",
            R.drawable.ic_pkg_angular,
            "npm install -g @angular/cli",
            "npm uninstall -g @angular/cli",
            "npm install -g @angular/cli",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/ng:" + TermuxConstants.TERMUX_HOME_DIR_PATH + "/.local/bin/ng",
            "4.8 ★",
            "28 MB",
            "ng",
            "ng --help",
            null
        ));

        // 5.4 Golang (Go)
        mAllPackages.add(new StorePackageItem(
            "golang",
            "Golang (Go)",
            "google",
            "Google • Lenguaje de Programación",
            "Lenguaje compilado, concurrente y de alto rendimiento creado por Google para desarrollo de sistemas y microservicios.",
            R.drawable.ic_pkg_golang,
            "pkg install -y golang",
            "pkg uninstall -y golang",
            "pkg install -y golang",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/go",
            "4.9 ★",
            "65 MB",
            "go",
            "go help",
            null
        ));
    }

    @Override
    public void onActionClick(StorePackageItem item, boolean isInstalled) {
        String commandToRun;
        if (isInstalled && item.getUpdateCommand() != null) {
            commandToRun = item.getUpdateCommand();
        } else {
            commandToRun = item.getInstallCommand();
        }
        executeShellCommandInTerminal(item, item.getTitle(), commandToRun, false);
    }

    @Override
    public void onSecondaryActionClick(StorePackageItem item) {
        if (item.getUninstallCommand() != null) {
            executeShellCommandInTerminal(item, item.getTitle() + " (Desinstalar)", item.getUninstallCommand(), true);
        }
    }

    @Override
    public void onItemClick(StorePackageItem item) {
        showPackageGuideDialog(item);
    }

    private void showPackageGuideDialog(StorePackageItem item) {
        StringBuilder message = new StringBuilder();
        message.append(item.getDescription()).append("\n\n");

        if (item.getInitGuide() != null && !item.getInitGuide().trim().isEmpty()) {
            message.append("Cómo inicializar / empezar:\n")
                   .append(item.getInitGuide()).append("\n\n");
        }

        if (item.getHelpGuide() != null && !item.getHelpGuide().trim().isEmpty()) {
            message.append("Ayuda y documentación:\n")
                   .append(item.getHelpGuide()).append("\n\n");
        }

        if (item.getQuickTip() != null && !item.getQuickTip().trim().isEmpty()) {
            message.append("Tip para principiantes:\n")
                   .append(item.getQuickTip());
        }

        boolean isInstalled = false;
        if (item.getBinaryCheckPath() != null) {
            for (String path : item.getBinaryCheckPath().split(":")) {
                if (new File(path).exists()) {
                    isInstalled = true;
                    break;
                }
            }
        }
        final boolean finalIsInstalled = isInstalled;

        String actionButtonLabel = item.getBinaryCheckPath() == null ? "Ejecutar" : (isInstalled ? "Actualizar" : "Instalar");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(item.getTitle())
            .setIcon(item.getIconResId())
            .setMessage(message.toString().trim())
            .setPositiveButton(actionButtonLabel, (d, which) -> onActionClick(item, finalIsInstalled))
            .setNeutralButton("Copiar comandos", (d, which) -> {
                StringBuilder copyText = new StringBuilder();
                if (item.getInitGuide() != null) {
                    copyText.append("# Inicializar:\n").append(item.getInitGuide()).append("\n\n");
                }
                if (item.getHelpGuide() != null) {
                    copyText.append("# Ayuda:\n").append(item.getHelpGuide()).append("\n");
                }
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Termux Comandos", copyText.toString().trim()));
                    Toast.makeText(this, "Comandos copiados al portapapeles", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cerrar", null)
            .show();

        if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        }
        if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL) != null) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.WHITE);
        }
        if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9AA0A6"));
        }
    }

    private void executeShellCommandInTerminal(@NonNull StorePackageItem item, String title, String shellCommand, boolean isUninstall) {
        if (shellCommand == null || shellCommand.trim().isEmpty()) return;

        String commandToSend = shellCommand.trim();

        // Si existe un archivo setup_zsh.sh residual previo, lo eliminamos
        if ("zsh".equals(item.getId())) {
            try {
                File homeScript = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, "setup_zsh.sh");
                if (homeScript.exists()) {
                    homeScript.delete();
                }
                File cacheScript = new File(new File(getCacheDir(), "store_scripts"), "setup_zsh.sh");
                if (cacheScript.exists()) {
                    cacheScript.delete();
                }
            } catch (Exception ignored) {
            }
        }

        Intent termuxIntent = new Intent(this, TermuxActivity.class);
        termuxIntent.setAction(TermuxActivity.ACTION_EXECUTE_IN_CURRENT_SESSION);
        termuxIntent.putExtra(TermuxActivity.EXTRA_COMMAND, commandToSend);
        termuxIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(termuxIntent);

        Toast.makeText(this, "Ejecutando en la sesión actual: " + title, Toast.LENGTH_SHORT).show();
    }
}
