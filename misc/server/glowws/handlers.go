package glowws

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"unicode/utf8"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
)

type DBManager struct {
	db            *sql.DB
	isInitialized bool
}

var dbManagerInstance = new()

func GetDBManager() DBManager {
	return dbManagerInstance
}

type User struct {
    Id int
    ChatId int
}

func new() DBManager {
	localDbRef, err := sql.Open("sqlite3", "/sqlite3/tg.db")
	if err != nil {
		panic("Error initializing db")
	} else {
		fmt.Print("DB Initialized successfully")
		_, err := localDbRef.Exec(`CREATE TABLE users(
  id INTEGER PRIMARY KEY, 
  chatId INTEGER);`)
		if err != nil {
			fmt.Printf("Error creating table, err=%s", err)
		} else {
			fmt.Print("Table users created successfully")
		}
	}
	return DBManager{db: localDbRef, isInitialized: true}
}

func Models(w http.ResponseWriter, r *http.Request) {
	bytes, _ := os.ReadFile("static/glowws_models.json")
	w.Write(bytes)
}

func InitBot() {
    token, exists := os.LookupEnv("TG_BOT_API") 
    if (!exists) {
        fmt.Printf(`WARN: Environment variable "TG_BOT_API" not found`)
        return
    }

    bot, err := tgbotapi.NewBotAPI(token)
	if err != nil {
        fmt.Printf("ERROR: Could not initialize bot, err=%s\n", err)
        return
	}

	bot.Debug = true

	u := tgbotapi.NewUpdate(0)
	u.Timeout = 60

    updates := bot.GetUpdatesChan(u)

	for update := range updates {
		if update.Message != nil { 
            var user User
            err := dbManagerInstance.db.QueryRow("SELECT * FROM users WHERE id = $1", update.Message.From.ID).Scan(&user)
            if err != nil {
                if err != sql.ErrNoRows {
                    fmt.Printf("ERROR: Could not get rows, err=%s\n", err)
                    return
                }

                dbManagerInstance.db.Exec("INSERT INTO users (id, chatId) VALUES ($1, $2)", update.Message.From.ID, update.Message.Chat.ID)
            }
		}
	}
}

func Stats(w http.ResponseWriter, r *http.Request) {
    var form FeedbackForm

    bytes, err := io.ReadAll(r.Body) 
    if (err != nil) {
        fmt.Println("ERROR: Could not read from feedback body, err=%s\n", err)
        return
    }
    err = json.Unmarshal(bytes, &form)
    if (err != nil) {
        fmt.Println("ERROR: Could not unmarshal feedback request, err=%s\n", err)
        return
    }

    token, exists := os.LookupEnv("TG_BOT_API") 
    if (!exists) {
        fmt.Printf(`WARN: Environment variable "TG_BOT_API" not found`)
        return
    }

    bot, err := tgbotapi.NewBotAPI(token)
	if err != nil {
        fmt.Printf("ERROR: Could not initialize bot, err=%s\n", err)
        return
	}

	bot.Debug = true

	u := tgbotapi.NewUpdate(0)
	u.Timeout = 60


    rows, err := dbManagerInstance.db.Query("SELECT * FROM users")
    if err != nil {
        fmt.Printf("ERROR: Could not get rows, err=%s\n", err)
        return
    }
    for rows.Next() {
        var user User
        err := rows.Scan(&user.Id, &user.ChatId)
        if err != nil {
            fmt.Printf("Error scannind db row, err=%s", err)
            continue
        }
        str, entities := form.string()
        msg := tgbotapi.NewMessage(int64(user.ChatId), str) 
        msg.Entities = entities 
        bot.Send(msg)
    }
}

func (f FeedbackForm) string() (string, []tgbotapi.MessageEntity) {
    ent := []tgbotapi.MessageEntity{}
    head := f.Header 
    ent = append(ent, tgbotapi.MessageEntity{Type: "bold", Offset: 0, Length: utf8.RuneCountInString(head)})
    rate := "Rating: "+ fmt.Sprintf("%.1f", f.Rating) + " ⭐"
    ent = append(ent, tgbotapi.MessageEntity{Type: "italic", Offset: utf8.RuneCountInString(head), Length: utf8.RuneCountInString(rate)})
    return head + "\n" + rate + "\n" + f.Description, ent
}
