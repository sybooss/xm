from __future__ import annotations

from pathlib import Path
from time import sleep

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.edge.options import Options
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "output" / "playwright"


def by_button_text(driver, text: str):
    return driver.find_element(By.XPATH, f"//button[normalize-space()='{text}']")


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--disable-gpu")
    options.add_argument("--window-size=1600,2000")

    driver = webdriver.Edge(options=options)
    wait = WebDriverWait(driver, 30)

    try:
        driver.get("http://127.0.0.1:4173/")
        wait.until(EC.presence_of_element_located((By.XPATH, "//*[contains(text(),'多轮对话工作台')]")))

        by_button_text(driver, "我刚买的蓝牙耳机不想要了，可以退货吗？").click()
        by_button_text(driver, "发送并检索规则").click()
        wait.until(EC.presence_of_element_located((By.XPATH, "//*[contains(text(),'最近执行轨迹')]")))
        wait.until(EC.presence_of_element_located((By.XPATH, "//*[contains(text(),'最近引用依据')]")))
        sleep(1.5)
        driver.save_screenshot(str(OUT_DIR / "chat-stage.png"))

        by_button_text(driver, "知识库中台").click()
        wait.until(EC.presence_of_element_located((By.XPATH, "//*[contains(text(),'知识库检索调试')]")))
        sleep(1.0)
        driver.save_screenshot(str(OUT_DIR / "knowledge-stage.png"))
    finally:
        driver.quit()


if __name__ == "__main__":
    main()
