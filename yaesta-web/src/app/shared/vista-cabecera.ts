import { Component, Input } from '@angular/core';
@Component({
  selector: 'app-vista-cabecera',
  template: `
    <section class="vista-intro">
      <div class="intro-texto">
        <p class="intro-etiqueta"><span></span>{{ etiqueta }}</p>
        <h1>{{ titulo }}</h1>
        <p class="intro-descripcion">{{ descripcion }}</p>
        <div class="intro-sello">
          <svg viewBox="0 0 36 36" aria-hidden="true">
            <circle cx="18" cy="18" r="14" />
            <path d="M18 8v11l7 4" /></svg
          ><span>Tu tiempo también cuenta.</span>
        </div>
      </div>
      <div class="intro-arte" aria-hidden="true">
        <div class="intro-arco"><div [class]="'foto f-' + foto"></div></div>
        <div class="intro-circulo"><div [class]="'foto f-' + foto"></div></div>
        <div class="intro-reloj">
          <svg viewBox="0 0 70 70">
            <circle cx="35" cy="35" r="29" />
            <path d="M35 15v21l14 9" />
          </svg>
        </div>
        <span class="intro-pie">YAESTÁ · a tu ritmo</span>
      </div>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
        margin: 20px 0 30px;
      }
      .vista-intro {
        display: grid;
        grid-template-columns: minmax(0, 1.4fr) minmax(240px, 1fr);
        gap: 28px;
        align-items: center;
        min-height: 235px;
        padding: 28px 36px;
        background: #f1e6dd;
        border: 1px solid #ebddd0;
        border-radius: 36px 75px 38px 36px;
        overflow: hidden;
      }
      .intro-texto {
        position: relative;
        z-index: 1;
      }
      .intro-etiqueta {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 9px;
        letter-spacing: 1.8px;
        font-weight: 700;
        color: #9e573c;
        margin: 0 0 14px;
      }
      .intro-etiqueta > span {
        width: 6px;
        height: 6px;
        background: #a54f35;
        border-radius: 50%;
      }
      h1 {
        font-size: clamp(30px, 3.2vw, 47px);
        line-height: 1.12;
        letter-spacing: -1.6px;
        color: #30251f;
        margin: 0 0 14px;
        font-weight: 900;
      }
      .intro-descripcion {
        font-size: 14px;
        line-height: 1.7;
        color: #786152;
        max-width: 590px;
        margin: 0;
      }
      .intro-sello {
        display: flex;
        align-items: center;
        gap: 7px;
        color: #9e765c;
        font-size: 10px;
        margin-top: 16px;
      }
      .intro-sello svg {
        width: 20px;
        height: 20px;
        stroke: #a54f35;
        fill: none;
        stroke-width: 2;
        stroke-linecap: round;
      }
      .intro-arte {
        position: relative;
        height: 190px;
        max-width: 390px;
        width: 100%;
        justify-self: end;
      }
      .intro-arco {
        position: absolute;
        inset: 0 12% 0 3%;
        overflow: hidden;
        border-radius: 110px 95px 35px 70px;
      }
      .intro-circulo {
        position: absolute;
        right: -8px;
        bottom: 8px;
        width: 116px;
        height: 116px;
        overflow: hidden;
        border: 6px solid #f1e6dd;
        border-radius: 50%;
      }
      .intro-reloj {
        position: absolute;
        left: -11px;
        top: 28px;
        width: 75px;
        height: 75px;
        padding: 9px;
        background: #a9684b;
        border: 5px solid #f1e6dd;
        border-radius: 50%;
      }
      .intro-reloj svg {
        fill: none;
        stroke: #fff8ee;
        stroke-width: 2.4;
        stroke-linecap: round;
      }
      .intro-pie {
        position: absolute;
        left: 36px;
        bottom: 12px;
        background: #fffcf5e8;
        color: #764b34;
        border-radius: 25px;
        padding: 7px 14px;
        font-size: 10px;
        transform: rotate(-3deg);
      }
      @media (max-width: 700px) {
        .vista-intro {
          grid-template-columns: 1fr;
          gap: 20px;
          padding: 26px;
          border-radius: 28px 55px 28px 28px;
        }
        .intro-arte {
          max-width: 100%;
          height: 145px;
        }
        .intro-arco {
          inset: 0 9% 0 5%;
          border-radius: 100px 80px 30px 50px;
        }
        .intro-circulo {
          width: 100px;
          height: 100px;
        }
        .intro-reloj {
          left: 0;
          top: 15px;
          width: 65px;
          height: 65px;
        }
        .intro-sello {
          display: none;
        }
        h1 {
          font-size: 34px;
        }
        .intro-descripcion {
          font-size: 13px;
        }
      }
    `,
  ],
})
export class VistaCabecera {
  @Input() titulo = '';
  @Input() etiqueta = 'YAESTÁ';
  @Input() descripcion = '';
  @Input() foto = 3;
}
