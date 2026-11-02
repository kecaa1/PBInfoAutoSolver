import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI

app = FastAPI(title="PBInfo AutoSolver API")
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
MODEL = os.environ.get("OPENAI_MODEL", "gpt-4.1-mini")

class SolveRequest(BaseModel):
    problem_id: str
    statement: str

@app.post("/solve")
def solve(req: SolveRequest):
    if not os.environ.get("OPENAI_API_KEY"):
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
        response = client.responses.create(
            model=MODEL,
            input=prompt
        )
        answer = response.output_text
        return {"solution": answer, "explanation": "AI-generated answer. Verify it against the constraints and test cases before submitting."}
    except Exception as exc:
        raise HTTPException(502, "AI provider request failed") from exc
