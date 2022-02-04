// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"os"
	"time"

	"github.com/rs/zerolog"
)

// logger is the common logger in the application.
// Using zerolog for structured JSON logs.
var logger zerolog.Logger

func init() {
	zerolog.TimestampFieldName = "timestamp"
	zerolog.TimeFieldFormat = time.RFC3339Nano
	zerolog.LevelFieldName = "severity"
	zerolog.LevelFieldMarshalFunc = levelFieldMarshalFunc
	logger = zerolog.
		New(os.Stdout).
		With().
		Timestamp().
		Logger()
}

func levelFieldMarshalFunc(l zerolog.Level) string {
	switch l {
	case zerolog.TraceLevel:
		return "DEFAULT"
	case zerolog.DebugLevel:
		return "DEBUG"
	case zerolog.InfoLevel:
		return "INFO"
	case zerolog.WarnLevel:
		return "WARNING"
	case zerolog.ErrorLevel:
		return "ERROR"
	case zerolog.FatalLevel:
		return "CRITICAL"
	case zerolog.PanicLevel:
		return "ALERT"
	default:
		return "DEFAULT"
	}
}