package com.termux.app.store;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.List;

public class TermuxStoreActivity extends AppCompatActivity implements StorePackageAdapter.OnPackageActionListener {

    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private StorePackageAdapter mAdapter;
    private List<StorePackageItem> mPackages;
    private List<StorePackageItem> mAllPackages;
    private EditText mSearchInput;
    private ImageView mClearButton;
    private String mCurrentCategoryFilter = null;

    private TextView mChipForYou;
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
        getWindow().setStatusBarColor(Color.parseColor("#131314"));
        getWindow().setNavigationBarColor(Color.parseColor("#131314"));

        setContentView(R.layout.activity_store);

        mRecyclerView = findViewById(R.id.store_packages_recycler_view);
        mEmptyView = findViewById(R.id.store_empty_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        initPackageList();
        mAllPackages = new ArrayList<>(mPackages);
        mAdapter = new StorePackageAdapter(mPackages, this);
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
        mChipTopTools = findViewById(R.id.chip_top_tools);
        mChipDev = findViewById(R.id.chip_dev);
        mChipSystem = findViewById(R.id.chip_system);
        mChipAiCli = findViewById(R.id.chip_ai_cli);

        setupChips();
    }

    private void setupChips() {
        if (mChipForYou != null) {
            mChipForYou.setOnClickListener(v -> selectCategoryChip(null));
        }
        if (mChipTopTools != null) {
            mChipTopTools.setOnClickListener(v -> selectCategoryChip("herramientas"));
        }
        if (mChipDev != null) {
            mChipDev.setOnClickListener(v -> selectCategoryChip("desarrollo"));
        }
        if (mChipSystem != null) {
            mChipSystem.setOnClickListener(v -> selectCategoryChip("sistema"));
        }
        if (mChipAiCli != null) {
            mChipAiCli.setOnClickListener(v -> selectCategoryChip("ia"));
        }
    }

    private void selectCategoryChip(@Nullable String category) {
        mCurrentCategoryFilter = category;
        updateChipVisuals();
        filterPackages(mSearchInput != null ? mSearchInput.getText().toString() : "", mCurrentCategoryFilter);
    }

    private void updateChipVisuals() {
        int selectedBg = R.drawable.bg_store_chip_selected;
        int unselectedBg = R.drawable.bg_store_chip_unselected;
        int selectedText = Color.parseColor("#AECBFA");
        int unselectedText = Color.parseColor("#9AA0A6");

        setChipState(mChipForYou, mCurrentCategoryFilter == null, selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipTopTools, "herramientas".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipDev, "desarrollo".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
        setChipState(mChipSystem, "sistema".equalsIgnoreCase(mCurrentCategoryFilter), selectedBg, unselectedBg, selectedText, unselectedText);
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

        for (StorePackageItem item : mAllPackages) {
            boolean matchesQuery = normalizedQuery.isEmpty() ||
                item.getTitle().toLowerCase().contains(normalizedQuery) ||
                item.getDescription().toLowerCase().contains(normalizedQuery) ||
                item.getCategory().toLowerCase().contains(normalizedQuery);

            boolean matchesCategory = categoryFilter == null || categoryFilter.trim().isEmpty() ||
                item.getCategory().toLowerCase().contains(categoryFilter.toLowerCase());

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
        mPackages = new ArrayList<>();

        // 1. Yazi File Manager
        mPackages.add(new StorePackageItem(
            "yazi",
            "Yazi",
            "Productividad • Terminal File Manager",
            "Navegador de archivos ultrarrápido escrito en Rust con soporte para vista previa gráfica y asíncrona.",
            R.drawable.ic_store_yazi,
            "pkg install yazi -y",
            "pkg uninstall yazi -y",
            "pkg install yazi -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/yazi",
            "4.9 ★",
            "14 MB"
        ));

        // 2. Antigravity CLI
        mPackages.add(new StorePackageItem(
            "antigravity-cli",
            "Antigravity CLI",
            "IA & Herramientas • Asistente de Código",
            "Agente inteligente para desarrollo de software avanzado, pairing y automatización completa.",
            R.drawable.ic_store_antigravity,
            "curl -fsSL https://antigravity.google/cli/install.sh | bash",
            "rm -rf ~/.gemini/antigravity-cli ~/.local/bin/agy",
            "curl -fsSL https://antigravity.google/cli/install.sh | bash",
            "/data/data/com.termux/files/home/.local/bin/agy",
            "4.9 ★",
            "22 MB"
        ));

        // 3. Node.js LTS
        mPackages.add(new StorePackageItem(
            "nodejs-lts",
            "Node.js LTS",
            "Desarrollo • Javascript Runtime",
            "Entorno de ejecución para JavaScript en el servidor (Long Term Support) con gestor de paquetes npm.",
            R.drawable.ic_pkg_code,
            "pkg install nodejs-lts -y",
            "pkg uninstall nodejs-lts -y",
            "pkg install nodejs-lts -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/node",
            "4.8 ★",
            "35 MB"
        ));

        // 4. Python 3
        mPackages.add(new StorePackageItem(
            "python",
            "Python 3",
            "Desarrollo • Intérprete & pip",
            "Entorno completo de Python con gestor de paquetes pip para scripts, ciencia de datos y automatización.",
            R.drawable.ic_pkg_python,
            "pkg install python -y",
            "pkg uninstall python -y",
            "pkg install python -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/python",
            "4.9 ★",
            "48 MB"
        ));

        // 5. Git
        mPackages.add(new StorePackageItem(
            "git",
            "Git",
            "Desarrollo • Control de Versiones",
            "Sistema de control de versiones distribuido rápido y escalable para repositorios y código fuente.",
            R.drawable.ic_pkg_git,
            "pkg install git -y",
            "pkg uninstall git -y",
            "pkg install git -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/git",
            "4.9 ★",
            "18 MB"
        ));

        // 6. Neovim
        mPackages.add(new StorePackageItem(
            "neovim",
            "Neovim",
            "Desarrollo • Editor de Código",
            "Editor de texto modal hiperextensible, rápido y configurable con soporte para plugins en Lua.",
            R.drawable.ic_pkg_code,
            "pkg install neovim -y",
            "pkg uninstall neovim -y",
            "pkg install neovim -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/nvim",
            "4.9 ★",
            "12 MB"
        ));

        // 7. Zsh & Oh My Zsh
        mPackages.add(new StorePackageItem(
            "zsh",
            "Zsh & Oh My Zsh",
            "Herramientas • Shell Avanzada",
            "Shell de comandos interactiva con autocompletado inteligente, resaltado de sintaxis y temas.",
            R.drawable.ic_pkg_terminal,
            "pkg install zsh curl -y",
            "pkg uninstall zsh -y",
            "pkg install zsh -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/zsh",
            "4.8 ★",
            "15 MB"
        ));

        // 8. htop
        mPackages.add(new StorePackageItem(
            "htop",
            "htop",
            "Herramientas • Monitor de Sistema",
            "Visor interactivo y monitor de procesos en tiempo real con uso de CPU, memoria y árbol de tareas.",
            R.drawable.ic_pkg_monitor,
            "pkg install htop -y",
            "pkg uninstall htop -y",
            "pkg install htop -y",
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/htop",
            "4.8 ★",
            "4 MB"
        ));

        // 9. Termux Storage Setup
        mPackages.add(new StorePackageItem(
            "termux-setup-storage",
            "Termux Storage",
            "Sistema • Almacenamiento Compartido",
            "Configura el acceso al almacenamiento compartido de Android (~/storage/shared, descargas y fotos).",
            R.drawable.ic_pkg_storage,
            "termux-setup-storage",
            null,
            null,
            null,
            "4.9 ★",
            "1 MB"
        ));

        // 10. Backup Termux
        mPackages.add(new StorePackageItem(
            "termux-backup",
            "Termux Backup",
            "Sistema • Copia de Seguridad",
            "Crea una copia de seguridad comprimida completa de home y usr en /sdcard/Download/termux/termux-backup.tar.gz.",
            R.drawable.ic_pkg_backup,
            "mkdir -p /sdcard/Download/termux && cd /data/data/com.termux/files && tar -zcvf /sdcard/Download/termux/termux-backup.tar.gz home usr",
            null,
            null,
            null,
            "4.9 ★",
            "2 MB"
        ));

        // 11. Restore Termux Backup
        mPackages.add(new StorePackageItem(
            "termux-restore",
            "Termux Restore",
            "Sistema • Restaurar Copia",
            "Restaura la copia de seguridad previa desde /sdcard/Download/termux/termux-backup.tar.gz.",
            R.drawable.ic_pkg_backup,
            "cd /data/data/com.termux/files && tar -zxvf /sdcard/Download/termux/termux-backup.tar.gz --recursive-unlink",
            null,
            null,
            null,
            "4.8 ★",
            "2 MB"
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
        executeShellCommandInTerminal(item.getTitle(), commandToRun);
    }

    @Override
    public void onSecondaryActionClick(StorePackageItem item) {
        if (item.getUninstallCommand() != null) {
            executeShellCommandInTerminal(item.getTitle() + " (Desinstalar)", item.getUninstallCommand());
        }
    }

    private void executeShellCommandInTerminal(String title, String shellCommand) {
        try {
            File scriptDir = new File(getCacheDir(), "store_scripts");
            if (!scriptDir.exists()) scriptDir.mkdirs();
            String uniqueName = "run_store_action_" + System.currentTimeMillis() + ".sh";
            File scriptFile = new File(scriptDir, uniqueName);

            String scriptContent = "#!/data/data/com.termux/files/usr/bin/bash\n" +
                "sleep 0.15\n" +
                "draw_divider() {\n" +
                "  shopt -s checkwinsize 2>/dev/null\n" +
                "  (:;:) 2>/dev/null\n" +
                "  local cols=$(tput cols 2>/dev/null)\n" +
                "  [ -z \"$cols\" ] || [ \"$cols\" -le 0 ] 2>/dev/null && cols=$(stty size 2>/dev/null | awk '{print $2}')\n" +
                "  [ -z \"$cols\" ] || [ \"$cols\" -le 0 ] 2>/dev/null && cols=${COLUMNS:-0}\n" +
                "  [ -z \"$cols\" ] || [ \"$cols\" -le 0 ] 2>/dev/null && cols=45\n" +
                "  local target=$((cols > 2 ? cols - 1 : cols))\n" +
                "  local d=\"────────────────────────────────────────────────────────────────────────────────────────────────────\"\n" +
                "  local line=\"\"\n" +
                "  while [ ${#line} -lt \"$target\" ]; do line=\"${line}${d}\"; done\n" +
                "  echo -e \"\\033[90m${line:0:$target}\\033[0m\"\n" +
                "}\n" +
                "echo -e '\\033[1;32m●\\033[0m \\033[1;33mTermux Play Store: " + title + "\\033[0m'\n" +
                "echo -e '\\033[1;34m$ " + shellCommand.replace("'", "'\\''") + "\\033[0m'\n" +
                "draw_divider\n" +
                shellCommand + "\n" +
                "EXIT_CODE=$?\n" +
                "draw_divider\n" +
                "if [ $EXIT_CODE -eq 0 ]; then\n" +
                "  echo -e '\\033[1;32m●\\033[0m \\033[1;33mProceso finalizado con éxito (código 0)\\033[0m'\n" +
                "else\n" +
                "  echo -e '\\033[1;31m●\\033[0m \\033[1;33mError al ejecutar (código '$EXIT_CODE')\\033[0m'\n" +
                "fi\n" +
                "echo ''\n" +
                "echo -e '\\033[1;37mSesión activa para auditar y depurar\\033[0m'\n" +
                "echo -e '\\033[1;37mEscribe \\033[1;34mexit\\033[0;1;37m cuando desees cerrar esta sesión manualmente\\033[0m'\n" +
                "echo ''\n" +
                "if [ -n \"$SHELL\" ] && [ -x \"$SHELL\" ]; then\n" +
                "  exec \"$SHELL\"\n" +
                "elif [ -x /data/data/com.termux/files/usr/bin/bash ]; then\n" +
                "  exec /data/data/com.termux/files/usr/bin/bash\n" +
                "elif [ -x /data/data/com.termux/files/usr/bin/sh ]; then\n" +
                "  exec /data/data/com.termux/files/usr/bin/sh\n" +
                "else\n" +
                "  exec /system/bin/sh\n" +
                "fi\n";

            try (FileOutputStream fos = new FileOutputStream(scriptFile)) {
                fos.write(scriptContent.getBytes());
            }
            scriptFile.setExecutable(true, false);

            final Uri scriptUri = UriUtils.getFileUri(scriptFile.getAbsolutePath());
            Intent executeIntent = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, scriptUri);
            executeIntent.setClass(this, TermuxService.class);
            executeIntent.putExtra(TERMUX_SERVICE.EXTRA_SHELL_NAME, title);
            startService(executeIntent);

            Intent termuxIntent = new Intent(this, TermuxActivity.class);
            termuxIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(termuxIntent);

            Toast.makeText(this, "Ejecutando en la terminal: " + title, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error preparando script: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
