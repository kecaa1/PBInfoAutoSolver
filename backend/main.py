import os
import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="PBInfo AutoSolver API")
GROQ_API_KEY = os.environ.get("GROQ_API_KEY")
MODEL = os.environ.get("GROQ_MODEL", "llama-3.3-70b-versatile")
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"


class SolveRequest(BaseModel):
    problem_id: str
    statement: str


@app.post("/solve")
def solve(req: SolveRequest):
    if not GROQ_API_KEY:
        raise HTTPException(503, "AI service is not configured: set GROQ_API_KEY")
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
        response = requests.post(
            GROQ_URL,
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": MODEL,
                "messages": [
                    {
                        "role": "system",
                        "content": "You are an expert competitive-programming assistant. Produce C++17 solutions.",
                    },
                    {"role": "user", "content": prompt},
                ],
                "temperature": 0.2,
            },
            timeout=100,
        )
        response.raise_for_status()
        data = response.json()
        answer = data["choices"][0]["message"]["content"]
        if isinstance(answer, list):
            answer = "".join(
                item.get("text", "") for item in answer if isinstance(item, dict)
            )
        answer = (answer or "").strip()
        if not answer:
            raise ValueError("Groq returned an empty response")
        return {
            "solution": answer,
            "explanation": "AI-generated answer. Verify it against the constraints and test cases before submitting.",
        }
    except requests.HTTPError as exc:
        status = exc.response.status_code if exc.response is not None else 502
        try:
            error_data = exc.response.json() if exc.response is not None else {}
            error = error_data.get("error", {})
            message = error.get("message", "No error message returned")
            if isinstance(message, dict):
                message = str(message)
        except (ValueError, AttributeError):
            message = exc.response.text[:1000] if exc.response is not None else str(exc)
        raise HTTPException(502, f"Groq HTTP {status}: {message}") from exc
    except requests.RequestException as exc:
        raise HTTPException(502, f"Could not reach Groq: {type(exc).__name__}") from exc
    except (KeyError, IndexError, TypeError, ValueError) as exc:
        raise HTTPException(502, "Groq returned an unexpected response") from exc
