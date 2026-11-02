import os
import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="PBInfo AutoSolver API")
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")
MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")

class SolveRequest(BaseModel):
    problem_id: str
    statement: str

@app.post("/solve")
def solve(req: SolveRequest):
    if not GEMINI_API_KEY:
        raise HTTPException(503, "AI service is not configured")
    if not req.statement.strip() or len(req.statement) > 30000:
        raise HTTPException(400, "Problem statement is empty or too long")

    prompt = f"""Solve this programming exercise in C++17.
Return a correct, efficient solution and a concise explanation.
Pay attention to input/output format and constraints. Do not invent constraints.
If the statement is ambiguous, explain the ambiguity rather than guessing.

Problem ID: {req.problem_id}
Statement:
{req.statement}
"""
    try:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent"
        response = requests.post(
            url,
            headers={"x-goog-api-key": GEMINI_API_KEY, "Content-Type": "application/json"},
            json={"contents": [{"parts": [{"text": prompt}]}]},
            timeout=80,
        )
        response.raise_for_status()
        data = response.json()
        answer = "".join(
            part.get("text", "")
            for part in data.get("candidates", [{}])[0].get("content", {}).get("parts", [])
        ).strip()
        if not answer:
            raise ValueError("Gemini returned an empty response")
        return {
            "solution": answer,
            "explanation": "AI-generated answer. Verify it against the constraints and test cases before submitting."
        }
    except requests.HTTPError as exc:
        status = exc.response.status_code if exc.response is not None else 502
        try:
            error_data = exc.response.json() if exc.response is not None else {}
            message = error_data.get("error", {}).get("message", "No error message returned")
        except (ValueError, AttributeError):
            message = (exc.response.text[:1000] if exc.response is not None else str(exc))
        # Return Google's diagnostic without ever including the API key.
        raise HTTPException(502, f"Gemini HTTP {status}: {message}") from exc
    except requests.RequestException as exc:
        raise HTTPException(502, f"Could not reach Gemini: {type(exc).__name__}") from exc
    except (KeyError, IndexError, ValueError) as exc:
        raise HTTPException(502, "Gemini returned an unexpected response") from exc
