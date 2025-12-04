package ru.kpfu.itis.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JComboBox<String> characterCombo;
    private JButton loginButton;
    private String username;
    private String characterType;
    private boolean loggedIn = false;

    public LoginWindow() {
        setTitle("Diamond Dungeons - Вход");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        // Главная панель с отступами
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Заголовок
        JLabel titleLabel = new JLabel("Вход в игру", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Центральная панель с полями
        JPanel centerPanel = new JPanel(new GridLayout(4, 1, 10, 10));

        // Поле имени пользователя
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel usernameLabel = new JLabel("Имя игрока:");
        usernameField = new JTextField(15);
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);

        // Выбор персонажа
        JPanel characterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel characterLabel = new JLabel("Персонаж:");
        String[] characters = {"Красный воин", "Синий маг", "Зеленый плут"};
        characterCombo = new JComboBox<>(characters);
        characterPanel.add(characterLabel);
        characterPanel.add(characterCombo);

        // Описание персонажей
        JTextArea descriptionArea = new JTextArea(3, 30);
        descriptionArea.setText("Красный воин: +1 жизнь\nСиний маг: видит ловушки\nЗеленый плут: быстрый сбор");
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setBackground(new Color(240, 240, 240));

        // Кнопка входа
        loginButton = new JButton("Войти в игру");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setBackground(new Color(76, 175, 80));
        loginButton.setForeground(Color.WHITE);

        // Слушатель кнопки
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        // Добавляем все на панель
        centerPanel.add(usernamePanel);
        centerPanel.add(characterPanel);
        centerPanel.add(new JScrollPane(descriptionArea));
        centerPanel.add(loginButton);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Информационная панель внизу
        JLabel infoLabel = new JLabel("Ожидание подключения к серверу...", SwingConstants.CENTER);
        mainPanel.add(infoLabel, BorderLayout.SOUTH);

        add(mainPanel);

        // Настраиваем Enter для быстрого входа
        getRootPane().setDefaultButton(loginButton);
    }

    private void login() {
        username = usernameField.getText().trim();
        characterType = (String) characterCombo.getSelectedItem();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Введите имя игрока!",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (username.length() < 2 || username.length() > 15) {
            JOptionPane.showMessageDialog(this,
                    "Имя должно быть от 2 до 15 символов!",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        loggedIn = true;
        dispose(); // Закрываем окно входа
    }

    public String getUsername() {
        return username;
    }

    public String getCharacterType() {
        return characterType;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginWindow loginWindow = new LoginWindow();
            loginWindow.setVisible(true);
        });
    }
}
