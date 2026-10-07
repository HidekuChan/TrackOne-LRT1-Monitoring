# TrackOne: LRT-1 Congestion & Delay Monitoring System

> **CSS123P — AM15 (Group 10)**  
> Mapúa University — School of Information Technology

---

## Team Members
- **Taroma, Hideki**
- **Guevarra, Jymes**
- **Que, Marco**
- **Samaniego, Justin**

---

##  Project Overview
TrackOne is an automated, real-time platform congestion monitoring system for the LRT-1 transit line. It translates simulated tap transactions (`IN`/`OUT`) into actionable, visual platform load metrics across all 25 LRT-1 stations.

---

## System Architecture & Data Model

### Database Schema (Entity Relationship Diagram)
- **`STATIONS`**: Manages station capacity and names across the line.
- **`TAP_LOGS`**: Records tap entry/exit types, travel direction (`NB`/`SB`), and timestamps linked to station records.

### Software Architecture
- **`Main.java`**: System bootstrapper.
- **`SimulationController.java`**: Controls the simulation clock, schedule rules, and background tasks.
- **`LrtModel.java`**: Manages MySQL connection, generates mock tap transactions, and queries congestion data.
- **`DashboardView.java`**: Swing GUI displaying live station tables, log area, and station filtering.

---

## Repository Structure
- `/src` — Core Java source files (`Main`, `SimulationController`, `LrtModel`, `DashboardView`).
- `/database` — Database scripts (`Database.sql`) and ERD diagram (`ERD.png`).
- `/docs` — System UML Architecture Diagram and presentation slides (`PPT.pdf`).

---

## Tech Stack
- **Language:** Java 21 / Swing GUI
- **Database:** MySQL
