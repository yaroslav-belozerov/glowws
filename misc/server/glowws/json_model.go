package glowws

type FeedbackForm struct {
    Header string       `json:"header"`
    Rating float64      `json:"rating"`
    Description string  `json:"desc"`
}
