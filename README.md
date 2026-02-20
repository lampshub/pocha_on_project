- 요구사항 정리
https://www.figma.com/board/GzqR7PGYp6NYsxppcBJbq6/beyound_23_2nd_2?node-id=81-654&t=Lhius0xuJmeNkOWy-0
- ERD설계
https://www.erdcloud.com/d/PR7aX8xBRa9ghzgs6
- 요구사항명세서 & WBD
https://docs.google.com/spreadsheets/d/1B3iMINWCP4MzMXL3EjZEhiUVZBddpXU1HHjzwwbAqNk/edit?gid=0#gid=0



# 🍺 Pocha_On 오더시스템 - 실시간 주문 및 실시간 채팅 서비스
---

**팀원**

 [이수림](https://github.com/sssurim-png) | 👑 [정명진](https://github.com/jmj010702) |  [홍진희](https://github.com/lampshub) |  [황주완](https://github.com/HwangJwan)

---

## 1. 프로젝트 소개

## 1.1 서비스 제작 배경 및 목표

기존 매장 주문 시스템은 단순 주문 전달에 집중되어 있으며,  
고객 간의 소통 기능은 제공하지 않는 경우가 대부분입니다.

또한 주문과 소통이 하나의 채팅 시스템으로 통합될 경우,
비즈니스 로직과 사용자 간 자유 대화가 혼재되어
서비스 구조가 복잡해질 수 있다는 한계가 있습니다.

Pocha_ON은 이러한 문제를 해결하기 위해

- 매장 ↔ 테이블 간에는 **실시간 주문 및 상태 알림(단방향)**
- 테이블 ↔ 테이블 간에는 **독립된 실시간 채팅**

으로 기능을 분리 설계했습니다.

이를 통해 주문 처리의 안정성을 확보하면서도,
고객 간 자유로운 소통 경험을 동시에 제공하는 것을 목표로 했습니다.

## 1.2 서비스 소개

**Pocha_On**은 매장(Owner)과 테이블(Customer) 간의 **실시간 주문/알림**과  
테이블과 테이블 간의 **실시간 채팅**을 제공하는 서비스입니다.

주문 영역은 **단반향 실시간 처리**로 정확한 처리에 집중하고,
채팅 영역은 고객 간 소통을 위한 **독립적인 실시간 서비스**로 설계했습니다.

> 💡 **서비스 핵심 특징**
> - 매장 ↔ 테이블 간 실시간 주문 및 상태 알림
> - 테이블 ↔ 테이블 간 실시간 채팅
> - Redis + SSE를 활용한 안정적인 실시간 통신 구조

---

## 2. 기술 스택

### Backend
<p>
<img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
<img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/>
<img src="https://img.shields.io/badge/JPA-Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white"/>
</p>

### Realtime / Messaging
<P>
<img src="https://img.shields.io/badge/Redis-Pub%2FSub-DC382D?style=for-the-badge&logo=redis&logoColor=white"/>
<img src="https://img.shields.io/badge/SSE-Server%20Sent%20Events-000000?style=for-the-badge"/>
</P>

### Database
<p>
<img src="https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>
</p>

### Frontend
<p>
<img src="https://img.shields.io/badge/Vue.js-3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white"/>
<img src="https://img.shields.io/badge/Pinia-State%20Management-FADA5E?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Axios-HTTP%20Client-5A29E4?style=for-the-badge"/>
</p>

### DevOps / Tool
<p>
<img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white"/>
<img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"/>
<img src="https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white"/>
<img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white"/>
</p>

---

## 3. 분석 및 설계
**[요구사항 명세서](https://docs.google.com/spreadsheets/d/1B3iMINWCP4MzMXL3EjZEhiUVZBddpXU1HHjzwwbAqNk/edit?gid=0#gid=0)**

프로젝트 기능 정의 및 상세 요구사항 정리
 
**[ERD 설계](https://www.erdcloud.com/d/PR7aX8xBRa9ghzgs6)**

전체 프로젝트 일정과 작업 흐름 구성

---

## 4. 주요 기능

### 🔹 실시간 주문
- 고객이 주문 생성 시 매장에 **즉시 알림 전송**
- 주문 처리 흐름에 집중한 단방향 구조

### 🔹 실시간 채팅 (Service Core Feature)
- 테이블 ↔ 테이블 간 실시간 채팅 제공
- 주문 기능과 분리된 독립 서비스
- 다중 테이블 환경에서 실시간 메시지 송수신 지원
- 메시지 전송 시 Redis를 활용한 이벤트 처리

### 🔹 실시간 알림 (ON / OFF 지원)
- SSE(Server-Sent Events)를 활용한 실시간 알림
- Redis를 통해 사용자별 알림 설정 관리
- 알림 ON/OFF 기능 제공 (개인화된 알림 제어)

---

## 5. 시스템 아키텍처

Client (Table / Owner)
│
│ HTTP / SSE
▼
Spring Boot Server
│
├─ 실시간 주문 API (단방향)
├─ 주문 상태 알림 (SSE)
├─ 테이블 간 채팅 API
│
▼
Redis (Pub/Sub, 알림 & 채팅 이벤트)
│
▼
MySQL (주문 / 사용자 데이터)

---

## 5. 실시간 기능 구현 방식

### 🔹 실시간 주문
- 주문 이벤트 발생 시 Redis를 통해 매장에 즉시 전달

---

### 🔹 테이블 간 실시간 채팅 & 알림
- Redis Pub/Sub 기반 메시지 브로드캐스팅
- 테이블 ID 기반 채팅 채널 분리
- 주문 데이터와 완전히 독립된 구조
- 알림 ON/OFF 설정을 Redis로 관리

---

## 6. 설계에서 중점적으로 고려한 부분

- 주문과 채팅의 **책임 분리**
- 단방향 주문 흐름을 통한 비즈니스 로직 단순화
- 실시간 처리에서 서버 부하 최소화
- Redis와 RDB의 명확한 역할 구분

